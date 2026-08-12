package com.company.banking.dashboard.application;

import com.company.banking.dashboard.domain.DashboardMetrics;
import com.company.banking.dashboard.domain.DashboardMetricsRepository;
import com.company.banking.dashboard.domain.PeriodVolumes;
import com.company.banking.dashboard.domain.VolumeStats;
import com.company.banking.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds admin dashboard metrics from aggregate repository queries.
 */
@Service
public class DashboardService {

    private final DashboardMetricsRepository dashboardMetricsRepository;
    private final Clock clock;

    public DashboardService(DashboardMetricsRepository dashboardMetricsRepository, Clock clock) {
        this.dashboardMetricsRepository = dashboardMetricsRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardMetricsResponse getMetrics() {
        Instant now = clock.instant();
        LocalDate todayUtc = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Instant dailyFrom = todayUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weeklyFrom = todayUtc.minusDays(6).atStartOfDay().toInstant(ZoneOffset.UTC);

        BigDecimal totalBalance = dashboardMetricsRepository.sumTotalBalance();
        if (totalBalance == null) {
            totalBalance = BigDecimal.ZERO.setScale(2);
        }

        DashboardMetrics metrics = new DashboardMetrics(
                dashboardMetricsRepository.countTotalCustomers(),
                dashboardMetricsRepository.countActiveCustomers(),
                dashboardMetricsRepository.countTotalAccounts(),
                dashboardMetricsRepository.countActiveAccounts(),
                totalBalance,
                toPeriodVolumes(dashboardMetricsRepository.volumeByTypeSince(dailyFrom)),
                toPeriodVolumes(dashboardMetricsRepository.volumeByTypeSince(weeklyFrom))
        );

        return DashboardMetricsMapper.toResponse(metrics);
    }

    /**
     * OpenAPI {@code withdrawals} ← ledger {@code WITHDRAW};
     * {@code transfers} ← ledger {@code DEBIT} only (paired CREDIT is not double-counted).
     */
    static PeriodVolumes toPeriodVolumes(Map<String, VolumeStats> byType) {
        return new PeriodVolumes(
                byType.getOrDefault(TransactionType.DEPOSIT.name(), VolumeStats.ZERO),
                byType.getOrDefault(TransactionType.WITHDRAW.name(), VolumeStats.ZERO),
                byType.getOrDefault(TransactionType.DEBIT.name(), VolumeStats.ZERO)
        );
    }
}
