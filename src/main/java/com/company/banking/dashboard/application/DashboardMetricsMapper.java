package com.company.banking.dashboard.application;

import com.company.banking.dashboard.domain.DashboardMetrics;
import com.company.banking.dashboard.domain.PeriodVolumes;
import com.company.banking.dashboard.domain.VolumeStats;

/**
 * Maps domain metrics to OpenAPI response DTOs.
 */
public final class DashboardMetricsMapper {

    private DashboardMetricsMapper() {
    }

    public static DashboardMetricsResponse toResponse(DashboardMetrics metrics) {
        return new DashboardMetricsResponse(
                metrics.totalCustomers(),
                metrics.activeCustomers(),
                metrics.totalAccounts(),
                metrics.activeAccounts(),
                metrics.totalBalance(),
                toPeriod(metrics.daily()),
                toPeriod(metrics.weekly())
        );
    }

    private static PeriodVolumeResponse toPeriod(PeriodVolumes volumes) {
        return new PeriodVolumeResponse(
                toVolume(volumes.deposits()),
                toVolume(volumes.withdrawals()),
                toVolume(volumes.transfers())
        );
    }

    private static VolumeResponse toVolume(VolumeStats stats) {
        return new VolumeResponse(stats.count(), stats.amount());
    }
}
