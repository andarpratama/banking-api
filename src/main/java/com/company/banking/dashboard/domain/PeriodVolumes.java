package com.company.banking.dashboard.domain;

import java.util.Objects;

/**
 * Deposit / withdraw / transfer volumes for a single reporting window.
 */
public record PeriodVolumes(VolumeStats deposits, VolumeStats withdrawals, VolumeStats transfers) {

    public PeriodVolumes {
        Objects.requireNonNull(deposits, "deposits");
        Objects.requireNonNull(withdrawals, "withdrawals");
        Objects.requireNonNull(transfers, "transfers");
    }
}
