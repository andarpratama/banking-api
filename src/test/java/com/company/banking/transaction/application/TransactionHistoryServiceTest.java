package com.company.banking.transaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.company.banking.common.pagination.PageResponse;
import com.company.banking.security.SecurityContextHelper;
import com.company.banking.transaction.domain.Transaction;
import com.company.banking.transaction.domain.TransactionRepository;
import com.company.banking.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionHistoryServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private SecurityContextHelper securityContextHelper;
    private TransactionHistoryService historyService;

    private UUID customerId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        securityContextHelper = mock(SecurityContextHelper.class);
        historyService = new TransactionHistoryService(
                accountRepository,
                transactionRepository,
                new TransactionMapper(),
                securityContextHelper
        );
        customerId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    @Test
    void returnsPaginatedHistoryForOwner() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount()));
        when(securityContextHelper.isAdmin()).thenReturn(false);
        when(securityContextHelper.isOwner(customerId)).thenReturn(true);

        Instant createdAt = Instant.parse("2026-08-04T12:00:00Z");
        Transaction deposit = Transaction.deposit(
                UUID.randomUUID(),
                accountId,
                Money.of(new BigDecimal("50.00")),
                Money.of(new BigDecimal("150.00")),
                "Cash deposit",
                createdAt
        );
        when(transactionRepository.findByAccountFiltered(
                eq(accountId),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                eq(20),
                eq("createdAt"),
                eq("DESC")
        )).thenReturn(List.of(deposit));
        when(transactionRepository.countByAccountFiltered(
                eq(accountId), isNull(), isNull(), isNull(), isNull(), isNull()
        )).thenReturn(1L);

        PageResponse<TransactionResponse> page = historyService.getHistory(
                accountId, null, null, null, null, null, null, null, null
        );

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().getTransactionType()).isEqualTo("DEPOSIT");
        assertThat(page.content().getFirst().getAmount()).isEqualByComparingTo("50.00");
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.currentPage()).isEqualTo(0);
        assertThat(page.pageSize()).isEqualTo(20);
    }

    @Test
    void appliesTypeAndDateAndAmountFilters() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount()));
        when(securityContextHelper.isAdmin()).thenReturn(true);

        when(transactionRepository.findByAccountFiltered(
                eq(accountId),
                eq(TransactionType.DEPOSIT),
                any(Instant.class),
                any(Instant.class),
                eq(new BigDecimal("10.00")),
                eq(new BigDecimal("100.00")),
                eq(1),
                eq(10),
                eq("amount"),
                eq("ASC")
        )).thenReturn(List.of());
        when(transactionRepository.countByAccountFiltered(
                eq(accountId),
                eq(TransactionType.DEPOSIT),
                any(Instant.class),
                any(Instant.class),
                eq(new BigDecimal("10.00")),
                eq(new BigDecimal("100.00"))
        )).thenReturn(0L);

        PageResponse<TransactionResponse> page = historyService.getHistory(
                accountId,
                1,
                10,
                "amount,asc",
                TransactionType.DEPOSIT,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 4),
                new BigDecimal("10.00"),
                new BigDecimal("100.00")
        );

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.currentPage()).isEqualTo(1);
        assertThat(page.pageSize()).isEqualTo(10);
        verify(transactionRepository).findByAccountFiltered(
                eq(accountId),
                eq(TransactionType.DEPOSIT),
                any(Instant.class),
                any(Instant.class),
                eq(new BigDecimal("10.00")),
                eq(new BigDecimal("100.00")),
                eq(1),
                eq(10),
                eq("amount"),
                eq("ASC")
        );
    }

    @Test
    void rejectsNonOwnerNonAdmin() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount()));
        when(securityContextHelper.isAdmin()).thenReturn(false);
        when(securityContextHelper.isOwner(customerId)).thenReturn(false);

        assertThatThrownBy(() -> historyService.getHistory(
                accountId, null, null, null, null, null, null, null, null
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
        verify(transactionRepository, never()).findByAccountFiltered(
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any()
        );
    }

    @Test
    void rejectsUnknownAccount() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> historyService.getHistory(
                accountId, null, null, null, null, null, null, null, null
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Test
    void rejectsInvalidAmountRange() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount()));
        when(securityContextHelper.isAdmin()).thenReturn(true);

        assertThatThrownBy(() -> historyService.getHistory(
                accountId,
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                new BigDecimal("10.00")
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void rejectsInvalidDateRange() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount()));
        when(securityContextHelper.isAdmin()).thenReturn(true);

        assertThatThrownBy(() -> historyService.getHistory(
                accountId,
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 1),
                null,
                null
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void rejectsUnsupportedSortProperty() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount()));
        when(securityContextHelper.isAdmin()).thenReturn(true);

        assertThatThrownBy(() -> historyService.getHistory(
                accountId, null, null, "description,asc", null, null, null, null, null
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
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

