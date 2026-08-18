package com.company.banking.transaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.banking.account.application.AccountCache;
import com.company.banking.account.domain.Account;
import com.company.banking.account.domain.AccountRepository;
import com.company.banking.account.domain.AccountStatus;
import com.company.banking.account.domain.AccountType;
import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.common.money.Money;
import com.company.banking.security.SecurityContextHelper;
import com.company.banking.transaction.domain.Transaction;
import com.company.banking.transaction.domain.TransactionRepository;
import com.company.banking.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WithdrawServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private SecurityContextHelper securityContextHelper;
    private WithdrawAuditPort withdrawAuditPort;
    private AccountCache accountCache;
    private WithdrawService withdrawService;

    private UUID customerId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        securityContextHelper = mock(SecurityContextHelper.class);
        withdrawAuditPort = mock(WithdrawAuditPort.class);
        accountCache = mock(AccountCache.class);
        withdrawService = new WithdrawService(
                accountRepository,
                transactionRepository,
                new TransactionMapper(),
                securityContextHelper,
                withdrawAuditPort,
                accountCache
        );
        customerId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    @Test
    void withdrawDebitsBalanceAndPersistsLedger() {
        Account active = sampleAccount(AccountStatus.ACTIVE, "100.00");
        when(securityContextHelper.isAdmin()).thenReturn(false);
        when(securityContextHelper.isOwner(customerId)).thenReturn(true);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(active));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        WithdrawRequest request = new WithdrawRequest(
                accountId,
                new BigDecimal("40.00"),
                "ATM withdrawal"
        );

        TransactionResponse response = withdrawService.withdraw(request);

        assertThat(response.getTransactionType()).isEqualTo(TransactionType.WITHDRAW.name());
        assertThat(response.getAmount()).isEqualByComparingTo("40.00");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo("60.00");
        assertThat(response.getReferenceId()).isNull();
        assertThat(response.getDescription()).isEqualTo("ATM withdrawal");
        verify(transactionRepository).save(any(Transaction.class));
        verify(withdrawAuditPort).onWithdraw(any(Transaction.class));
        verify(accountCache).evict(accountId);
    }

    @Test
    void withdrawAllowsExactBalanceBoundary() {
        Account active = sampleAccount(AccountStatus.ACTIVE, "100.00");
        when(securityContextHelper.isAdmin()).thenReturn(true);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(active));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        WithdrawRequest request = new WithdrawRequest(accountId, new BigDecimal("100.00"), null);

        TransactionResponse response = withdrawService.withdraw(request);

        assertThat(response.getBalanceAfter()).isEqualByComparingTo("0.00");
        assertThat(response.getAmount()).isEqualByComparingTo("100.00");
        verify(accountCache).evict(accountId);
    }

    @Test
    void withdrawRejectsOneCentOverBalance() {
        when(securityContextHelper.isAdmin()).thenReturn(true);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(
                sampleAccount(AccountStatus.ACTIVE, "100.00")
        ));

        WithdrawRequest request = new WithdrawRequest(accountId, new BigDecimal("100.01"), null);

        assertThatThrownBy(() -> withdrawService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
                    assertThat(be.getMessage()).contains("Available: 100.00");
                    assertThat(be.getMessage()).contains("Requested: 100.01");
                });
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdrawRejectsInvalidAmount() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(
                sampleAccount(AccountStatus.ACTIVE, "100.00")
        ));

        WithdrawRequest request = new WithdrawRequest(accountId, BigDecimal.ZERO, null);

        assertThatThrownBy(() -> withdrawService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_AMOUNT));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdrawRejectsFrozenAccount() {
        when(securityContextHelper.isAdmin()).thenReturn(true);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(
                sampleAccount(AccountStatus.FROZEN, "100.00")
        ));

        WithdrawRequest request = new WithdrawRequest(accountId, new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> withdrawService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_FROZEN));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdrawRejectsClosedAccount() {
        when(securityContextHelper.isAdmin()).thenReturn(true);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(
                sampleAccount(AccountStatus.CLOSED, "100.00")
        ));

        WithdrawRequest request = new WithdrawRequest(accountId, new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> withdrawService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_CLOSED));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdrawForbiddenForNonOwnerCustomer() {
        when(securityContextHelper.isAdmin()).thenReturn(false);
        when(securityContextHelper.isOwner(customerId)).thenReturn(false);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(
                sampleAccount(AccountStatus.ACTIVE, "100.00")
        ));

        WithdrawRequest request = new WithdrawRequest(accountId, new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> withdrawService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdrawThrowsWhenAccountMissing() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        WithdrawRequest request = new WithdrawRequest(accountId, new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> withdrawService.withdraw(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private Account sampleAccount(AccountStatus status, String balance) {
        Instant now = Instant.parse("2026-08-10T10:00:00Z");
        return new Account(
                accountId,
                customerId,
                "ACC-0000001",
                AccountType.SAVINGS,
                "USD",
                Money.of(new BigDecimal(balance)),
                status,
                0L,
                now,
                now
        );
    }
}
