package com.company.banking.dashboard.application;

import java.math.BigDecimal;

/**
 * Response body for {@code GET /api/v1/dashboard/metrics} (OpenAPI §6.1).
 */
public class DashboardMetricsResponse {

    private long totalCustomers;
    private long activeCustomers;
    private long totalAccounts;
    private long activeAccounts;
    private BigDecimal totalBalance;
    private PeriodVolumeResponse daily;
    private PeriodVolumeResponse weekly;

    public DashboardMetricsResponse() {
    }

    public DashboardMetricsResponse(
            long totalCustomers,
            long activeCustomers,
            long totalAccounts,
            long activeAccounts,
            BigDecimal totalBalance,
            PeriodVolumeResponse daily,
            PeriodVolumeResponse weekly
    ) {
        this.totalCustomers = totalCustomers;
        this.activeCustomers = activeCustomers;
        this.totalAccounts = totalAccounts;
        this.activeAccounts = activeAccounts;
        this.totalBalance = totalBalance;
        this.daily = daily;
        this.weekly = weekly;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getActiveCustomers() {
        return activeCustomers;
    }

    public void setActiveCustomers(long activeCustomers) {
        this.activeCustomers = activeCustomers;
    }

    public long getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(long totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public long getActiveAccounts() {
        return activeAccounts;
    }

    public void setActiveAccounts(long activeAccounts) {
        this.activeAccounts = activeAccounts;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public PeriodVolumeResponse getDaily() {
        return daily;
    }

    public void setDaily(PeriodVolumeResponse daily) {
        this.daily = daily;
    }

    public PeriodVolumeResponse getWeekly() {
        return weekly;
    }

    public void setWeekly(PeriodVolumeResponse weekly) {
        this.weekly = weekly;
    }
}
