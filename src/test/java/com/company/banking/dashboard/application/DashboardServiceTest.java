package com.company.banking.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.banking.dashboard.domain.DashboardMetricsRepository;
import com.company.banking.dashboard.domain.PeriodVolumes;
import com.company.banking.dashboard.domain.VolumeStats;
import com.company.banking.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DashboardServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-11T15:30:00Z");

    private DashboardMetricsRepository repository;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        repository = mock(DashboardMetricsRepository.class);
        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new DashboardService(repository, clock);
    }

    @Test
    void getMetricsMapsCountsBalanceAndVolumes() {
        when(repository.countTotalCustomers()).thenReturn(10L);
        when(repository.countActiveCustomers()).thenReturn(8L);
        when(repository.countTotalAccounts()).thenReturn(20L);
        when(repository.countActiveAccounts()).thenReturn(15L);
        when(repository.sumTotalBalance()).thenReturn(new BigDecimal("1234.56"));
        when(repository.volumeByTypeSince(any())).thenReturn(Map.of(
                TransactionType.DEPOSIT.name(), new VolumeStats(2L, new BigDecimal("100.00")),
                TransactionType.WITHDRAW.name(), new VolumeStats(1L, new BigDecimal("40.00")),
                TransactionType.DEBIT.name(), new VolumeStats(3L, new BigDecimal("250.00")),
                TransactionType.CREDIT.name(), new VolumeStats(3L, new BigDecimal("250.00"))
        ));

        DashboardMetricsResponse response = service.getMetrics();

        assertThat(response.getTotalCustomers()).isEqualTo(10L);
        assertThat(response.getActiveCustomers()).isEqualTo(8L);
        assertThat(response.getTotalAccounts()).isEqualTo(20L);
        assertThat(response.getActiveAccounts()).isEqualTo(15L);
        assertThat(response.getTotalBalance()).isEqualByComparingTo("1234.56");

        assertThat(response.getDaily().getDeposits().getCount()).isEqualTo(2L);
        assertThat(response.getDaily().getDeposits().getAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getDaily().getWithdrawals().getCount()).isEqualTo(1L);
        assertThat(response.getDaily().getWithdrawals().getAmount()).isEqualByComparingTo("40.00");
        assertThat(response.getDaily().getTransfers().getCount()).isEqualTo(3L);
        assertThat(response.getDaily().getTransfers().getAmount()).isEqualByComparingTo("250.00");

        assertThat(response.getWeekly().getTransfers().getCount()).isEqualTo(3L);
    }

    @Test
    void getMetricsUsesUtcCalendarDailyAndWeeklyWindows() {
        when(repository.countTotalCustomers()).thenReturn(0L);
        when(repository.countActiveCustomers()).thenReturn(0L);
        when(repository.countTotalAccounts()).thenReturn(0L);
        when(repository.countActiveAccounts()).thenReturn(0L);
        when(repository.sumTotalBalance()).thenReturn(null);
        when(repository.volumeByTypeSince(any())).thenReturn(Map.of());

        DashboardMetricsResponse response = service.getMetrics();

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository, times(2)).volumeByTypeSince(fromCaptor.capture());

        assertThat(fromCaptor.getAllValues()).containsExactly(
                Instant.parse("2026-08-11T00:00:00Z"),
                Instant.parse("2026-08-05T00:00:00Z")
        );
        assertThat(response.getTotalBalance()).isEqualByComparingTo("0.00");
        assertThat(response.getDaily().getDeposits().getCount()).isZero();
        assertThat(response.getDaily().getWithdrawals().getAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getWeekly().getTransfers().getCount()).isZero();
    }

    @Test
    void toPeriodVolumesIgnoresCreditLegsForTransfers() {
        PeriodVolumes period = DashboardService.toPeriodVolumes(Map.of(
                TransactionType.CREDIT.name(), new VolumeStats(9L, new BigDecimal("999.00"))
        ));

        assertThat(period.deposits()).isEqualTo(VolumeStats.ZERO);
        assertThat(period.withdrawals()).isEqualTo(VolumeStats.ZERO);
        assertThat(period.transfers()).isEqualTo(VolumeStats.ZERO);
    }
}
