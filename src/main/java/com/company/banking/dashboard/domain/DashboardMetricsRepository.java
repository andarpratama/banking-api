package com.company.banking.dashboard.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Read-model port for dashboard aggregate queries (index-aware, no N+1).
 */
public interface DashboardMetricsRepository {

    long countTotalCustomers();

    /**
     * Customers that are not soft-deleted ({@code is_deleted = false}).
     */
    long countActiveCustomers();

    long countTotalAccounts();

    /**
     * Accounts with status {@code ACTIVE}.
     */
    long countActiveAccounts();

    /**
     * Sum of all account balances (closed accounts typically hold zero).
     */
    BigDecimal sumTotalBalance();

    /**
     * Volume by ledger type for rows with {@code created_at >= fromInclusive}.
     * Keys are {@code DEPOSIT}, {@code WITHDRAW}, {@code DEBIT}, {@code CREDIT}.
     * Missing types are treated as zero by the caller.
     */
    Map<String, VolumeStats> volumeByTypeSince(Instant fromInclusive);
}
