package com.company.banking.transaction.domain;

import com.company.banking.common.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Transaction repository port — insert-only ledger with history queries.
 */
public interface TransactionRepository {

    /**
     * Persists a new immutable ledger row. Must not update existing rows.
     */
    Transaction save(Transaction transaction);

    /**
     * Paginated ledger history for an account with optional filters.
     *
     * @param type        optional transaction type filter
     * @param fromDate    optional inclusive lower bound on {@code createdAt}
     * @param toDate      optional inclusive upper bound on {@code createdAt}
     * @param minAmount   optional inclusive minimum amount
     * @param maxAmount   optional inclusive maximum amount
     * @param sortBy      domain/API sort property ({@code createdAt}, {@code amount}, {@code transactionType})
     * @param sortDirection {@code ASC} or {@code DESC}
     */
    List<Transaction> findByAccountFiltered(
            UUID accountId,
            TransactionType type,
            Instant fromDate,
            Instant toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    /**
     * Count of ledger rows matching the same filters as {@link #findByAccountFiltered}.
     */
    long countByAccountFiltered(
            UUID accountId,
            TransactionType type,
            Instant fromDate,
            Instant toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount
    );

    /**
     * All ledger rows for an account in {@code [fromDate, toDate]}, oldest first.
     */
    List<Transaction> findByAccountInPeriod(
            UUID accountId,
            Instant fromDate,
            Instant toDate
    );

    /**
     * {@code balanceAfter} of the latest ledger row strictly before {@code before}, if any.
     */
    Optional<Money> findBalanceAfterBefore(UUID accountId, Instant before);
}
