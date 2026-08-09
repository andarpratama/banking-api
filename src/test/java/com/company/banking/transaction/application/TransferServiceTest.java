package com.company.banking.transaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.mockito.ArgumentCaptor;

class TransferServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private SecurityContextHelper securityContextHelper;
    private TransferAuditPort transferAuditPort;
    private TransferService transferService;

    private UUID ownerId;
    private UUID otherCustomerId;
    private UUID sourceId;
    private UUID destinationId;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        securityContextHelper = mock(SecurityContextHelper.class);
        transferAuditPort = mock(TransferAuditPort.class);
        transferService = new TransferService(
                accountRepository,
                transactionRepository,
                new TransactionMapper(),
                securityContextHelper,
                transferAuditPort
        );
        ownerId = UUID.randomUUID();
        otherCustomerId = UUID.randomUUID();
        sourceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        destinationId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    }

    @Test
    void transferDebitsSourceCreditsDestinationWithSharedReference() {
        Account source = sampleAccount(sourceId, ownerId, AccountStatus.ACTIVE, "1000.00");
        Account destination = sampleAccount(destinationId, otherCustomerId, AccountStatus.ACTIVE, "100.00");
        when(securityContextHelper.isAdmin()).thenReturn(false);
        when(securityContextHelper.isOwner(ownerId)).thenReturn(true);
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(accountRepository.findById(destinationId)).thenReturn(Optional.of(destination));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferRequest request = new TransferRequest(
                sourceId,
                destinationId,
                new BigDecimal("250.00"),
                "Transfer to friend"
        );

        TransferResponse response = transferService.transfer(request);

        assertThat(response.getReferenceId()).isNotNull();
        assertThat(response.getSourceTransaction().getTransactionType())
                .isEqualTo(TransactionType.DEBIT.name());
        assertThat(response.getDestinationTransaction().getTransactionType())
                .isEqualTo(TransactionType.CREDIT.name());
        assertThat(response.getSourceTransaction().getAmount()).isEqualByComparingTo("250.00");
        assertThat(response.getDestinationTransaction().getAmount()).isEqualByComparingTo("250.00");
        assertThat(response.getSourceTransaction().getBalanceAfter()).isEqualByComparingTo("750.00");
        assertThat(response.getDestinationTransaction().getBalanceAfter()).isEqualByComparingTo("350.00");
        assertThat(response.getSourceTransaction().getReferenceId()).isEqualTo(response.getReferenceId());
        assertThat(response.getDestinationTransaction().getReferenceId())
                .isEqualTo(response.getReferenceId());

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        assertThat(txCaptor.getAllValues())
                .extracting(Transaction::referenceId)
                .containsOnly(response.getReferenceId());
        verify(transferAuditPort).onTransfer(any(), any(), any());
    }

    @Test
    void transferRejectsSameAccount() {
        TransferRequest request = new TransferRequest(
                sourceId,
                sourceId,
                new BigDecimal("10.00"),
                null
        );

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.SAME_ACCOUNT_TRANSFER);
                    assertThat(be.getMessage()).isEqualTo("Cannot transfer to same account");
                });
        verify(accountRepository, never()).findById(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferRejectsInsufficientBalance() {
        when(securityContextHelper.isAdmin()).thenReturn(true);
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(
                sampleAccount(sourceId, ownerId, AccountStatus.ACTIVE, "100.00")
        ));
        when(accountRepository.findById(destinationId)).thenReturn(Optional.of(
                sampleAccount(destinationId, otherCustomerId, AccountStatus.ACTIVE, "0.00")
        ));

        TransferRequest request = new TransferRequest(
                sourceId,
                destinationId,
                new BigDecimal("100.01"),
                null
        );

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferRejectsFrozenSource() {
        when(securityContextHelper.isAdmin()).thenReturn(true);
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(
                sampleAccount(sourceId, ownerId, AccountStatus.FROZEN, "100.00")
        ));
        when(accountRepository.findById(destinationId)).thenReturn(Optional.of(
                sampleAccount(destinationId, otherCustomerId, AccountStatus.ACTIVE, "0.00")
        ));

        TransferRequest request = new TransferRequest(
                sourceId,
                destinationId,
                new BigDecimal("10.00"),
                null
        );

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_FROZEN));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferForbiddenForNonOwnerCustomer() {
        when(securityContextHelper.isAdmin()).thenReturn(false);
        when(securityContextHelper.isOwner(ownerId)).thenReturn(false);
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(
                sampleAccount(sourceId, ownerId, AccountStatus.ACTIVE, "100.00")
        ));
        when(accountRepository.findById(destinationId)).thenReturn(Optional.of(
                sampleAccount(destinationId, otherCustomerId, AccountStatus.ACTIVE, "0.00")
        ));

        TransferRequest request = new TransferRequest(
                sourceId,
                destinationId,
                new BigDecimal("10.00"),
                null
        );

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferThrowsWhenSourceMissing() {
        when(accountRepository.findById(sourceId)).thenReturn(Optional.empty());

        TransferRequest request = new TransferRequest(
                sourceId,
                destinationId,
                new BigDecimal("10.00"),
                null
        );

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    void transferThrowsWhenDestinationMissing() {
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(
                sampleAccount(sourceId, ownerId, AccountStatus.ACTIVE, "100.00")
        ));
        when(accountRepository.findById(destinationId)).thenReturn(Optional.empty());

        TransferRequest request = new TransferRequest(
                sourceId,
                destinationId,
                new BigDecimal("10.00"),
                null
        );

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private Account sampleAccount(UUID id, UUID customerId, AccountStatus status, String balance) {
        Instant now = Instant.parse("2026-08-10T10:00:00Z");
        return new Account(
                id,
                customerId,
                "ACC-" + id.toString().substring(24),
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
