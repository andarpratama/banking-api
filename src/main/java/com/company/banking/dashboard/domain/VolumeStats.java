package com.company.banking.dashboard.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Aggregated count and amount for one transaction category in a time window.
 */
public record VolumeStats(long count, BigDecimal amount) {

    public static final VolumeStats ZERO = new VolumeStats(0L, BigDecimal.ZERO.setScale(2));

    public VolumeStats {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
        Objects.requireNonNull(amount, "amount");
        amount = amount.setScale(2);
    }
}
