package com.company.banking.transaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

class DepositServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private SecurityContextHelper securityContextHelper;
    private DepositAuditPort depositAuditPort;
    private DepositService depositService;

    private UUID customerId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        securityContextHelper = mock(SecurityContextHelper.class);
        depositAuditPort = mock(DepositAuditPort.class);
        depositService = new DepositService(
                accountRepository,
                transactionRepository,
                new TransactionMapper(),
                securityContextHelper,
                depositAuditPort
        );
        customerId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    @Test
    void depositCreditsBalanceAndPersistsLedger() {
        Account active = sampleAccount(AccountStatus.ACTIVE, "100.00");
        when(securityContextHelper.isAdmin()).thenReturn(false);
        when(securityContextHelper.isOwner(customerId)).thenReturn(true);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(active));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        DepositRequest request = new DepositRequest(
                accountId,
                new BigDecimal("50.00"),
                "Cash deposit at ATM"
        );

        TransactionResponse response = depositService.deposit(request);

        assertThat(response.getTransactionType()).isEqualTo(TransactionType.DEPOSIT.name());
        assertThat(response.getAmount()).isEqualByComparingTo("50.00");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo("150.00");
        assertThat(response.getReferenceId()).isNull();
        assertThat(response.getDescription()).isEqualTo("Cash deposit at ATM");
        verify(transactionRepository).save(any(Transaction.class));
        verify(depositAuditPort).onDeposit(any(Transaction.class));
    }

    @Test
    void depositRejectsInvalidAmount() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(
                sampleAccount(AccountStatus.ACTIVE, "100.00")
        ));

        DepositRequest request = new DepositRequest(accountId, BigDecimal.ZERO, null);

        assertThatThrownBy(() -> depositService.deposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_AMOUNT));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void depositRejectsFrozenAccount() {
        when(securityContextHelper.isAdmin()).thenReturn(true);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(
                sampleAccount(AccountStatus.FROZEN, "100.00")
        ));

        DepositRequest request = new DepositRequest(accountId, new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> depositService.deposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_FROZEN));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void depositRejectsClosedAccount() {
        when(securityContextHelper.isAdmin()).thenReturn(true);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(
                sampleAccount(AccountStatus.CLOSED, "100.00")
        ));

        DepositRequest request = new DepositRequest(accountId, new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> depositService.deposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_CLOSED));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void depositForbiddenForNonOwnerCustomer() {
        when(securityContextHelper.isAdmin()).thenReturn(false);
        when(securityContextHelper.isOwner(customerId)).thenReturn(false);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(
                sampleAccount(AccountStatus.ACTIVE, "100.00")
        ));

        DepositRequest request = new DepositRequest(accountId, new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> depositService.deposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void depositThrowsWhenAccountMissing() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        DepositRequest request = new DepositRequest(accountId, new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> depositService.deposit(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private Account sampleAccount(AccountStatus status, String balance) {
        Instant now = Instant.parse("2026-08-09T10:00:00Z");
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
