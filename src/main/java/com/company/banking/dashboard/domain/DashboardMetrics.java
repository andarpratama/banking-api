package com.company.banking.dashboard.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * System-wide dashboard metrics snapshot.
 */
public record DashboardMetrics(
        long totalCustomers,
        long activeCustomers,
        long totalAccounts,
        long activeAccounts,
        BigDecimal totalBalance,
        PeriodVolumes daily,
        PeriodVolumes weekly
) {

    public DashboardMetrics {
        if (totalCustomers < 0 || activeCustomers < 0 || totalAccounts < 0 || activeAccounts < 0) {
            throw new IllegalArgumentException("counts must be >= 0");
        }
        Objects.requireNonNull(totalBalance, "totalBalance");
        totalBalance = totalBalance.setScale(2);
        Objects.requireNonNull(daily, "daily");
        Objects.requireNonNull(weekly, "weekly");
    }
}
