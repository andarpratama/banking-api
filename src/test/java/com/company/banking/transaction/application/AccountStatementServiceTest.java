package com.company.banking.transaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountStatementServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private SecurityContextHelper securityContextHelper;
    private AccountStatementService statementService;

    private UUID customerId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        securityContextHelper = mock(SecurityContextHelper.class);
        statementService = new AccountStatementService(
                accountRepository,
                transactionRepository,
                securityContextHelper
        );
        customerId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    @Test
    void returnsStatementWithOpeningClosingAndTotals() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount()));
        when(securityContextHelper.isAdmin()).thenReturn(false);
        when(securityContextHelper.isOwner(customerId)).thenReturn(true);
        when(transactionRepository.findBalanceAfterBefore(eq(accountId), any(Instant.class)))
                .thenReturn(Optional.of(Money.of(new BigDecimal("4500.50"))));

        Instant depositAt = Instant.parse("2026-08-01T10:00:00Z");
        Instant withdrawAt = Instant.parse("2026-08-02T11:00:00Z");
        Transaction deposit = Transaction.deposit(
                UUID.randomUUID(),
                accountId,
                Money.of(new BigDecimal("1500.00")),
                Money.of(new BigDecimal("6000.50")),
                "Cash deposit",
                depositAt
        );
        Transaction withdraw = Transaction.withdraw(
                UUID.randomUUID(),
                accountId,
                Money.of(new BigDecimal("300.00")),
                Money.of(new BigDecimal("5700.50")),
                "ATM cash",
                withdrawAt
        );
        when(transactionRepository.findByAccountInPeriod(eq(accountId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(deposit, withdraw));

        StatementResponse statement = statementService.getStatement(
                accountId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 4)
        );

        assertThat(statement.getAccountId()).isEqualTo(accountId);
        assertThat(statement.getAccountNumber()).isEqualTo("ACC-0000001");
        assertThat(statement.getOpeningBalance()).isEqualByComparingTo("4500.50");
        assertThat(statement.getClosingBalance()).isEqualByComparingTo("5700.50");
        assertThat(statement.getTotalDeposits()).isEqualByComparingTo("1500.00");
        assertThat(statement.getTotalWithdrawals()).isEqualByComparingTo("300.00");
        assertThat(statement.getTotalDebits()).isEqualByComparingTo("0.00");
        assertThat(statement.getTotalCredits()).isEqualByComparingTo("0.00");
        assertThat(statement.getTransactions()).hasSize(2);
        assertThat(statement.getTransactions().getFirst().getType()).isEqualTo("DEPOSIT");
        assertThat(statement.getTransactions().getFirst().getBalance())
                .isEqualByComparingTo("6000.50");
        assertThat(statement.getStatementPeriod().getFrom())
                .isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
        assertThat(statement.getStatementPeriod().getTo())
                .isEqualTo(Instant.parse("2026-08-04T23:59:59.999999999Z"));
    }

    @Test
    void emptyPeriodUsesOpeningAsClosing() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount()));
        when(securityContextHelper.isAdmin()).thenReturn(true);
        when(transactionRepository.findBalanceAfterBefore(eq(accountId), any(Instant.class)))
                .thenReturn(Optional.of(Money.of(new BigDecimal("100.00"))));
        when(transactionRepository.findByAccountInPeriod(eq(accountId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        StatementResponse statement = statementService.getStatement(
                accountId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 4)
        );

        assertThat(statement.getOpeningBalance()).isEqualByComparingTo("100.00");
        assertThat(statement.getClosingBalance()).isEqualByComparingTo("100.00");
        assertThat(statement.getTransactions()).isEmpty();
    }

    @Test
    void rejectsMissingDates() {
        assertThatThrownBy(() -> statementService.getStatement(accountId, null, LocalDate.of(2026, 8, 4)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void rejectsInvalidDateRange() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount()));
        when(securityContextHelper.isAdmin()).thenReturn(true);

        assertThatThrownBy(() -> statementService.getStatement(
                accountId,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 1)
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void rejectsNonOwnerNonAdmin() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount()));
        when(securityContextHelper.isAdmin()).thenReturn(false);
        when(securityContextHelper.isOwner(customerId)).thenReturn(false);

        assertThatThrownBy(() -> statementService.getStatement(
                accountId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 4)
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
        verify(transactionRepository, never()).findByAccountInPeriod(any(), any(), any());
    }

    @Test
    void rejectsUnknownAccount() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statementService.getStatement(
                accountId,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 4)
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private Account sampleAccount() {
        Instant now = Instant.parse("2026-08-01T00:00:00Z");
        return new Account(
                accountId,
                customerId,
                "ACC-0000001",
                AccountType.CHECKING,
                "USD",
                Money.of(new BigDecimal("100.00")),
                AccountStatus.ACTIVE,
                0L,
                now,
                now
        );
    }
}
