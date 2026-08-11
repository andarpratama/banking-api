package com.company.banking.dashboard.application;

import java.math.BigDecimal;

/**
 * OpenAPI volume object: {@code count} + {@code amount}.
 */
public class VolumeResponse {

    private long count;
    private BigDecimal amount;

    public VolumeResponse() {
    }

    public VolumeResponse(long count, BigDecimal amount) {
        this.count = count;
        this.amount = amount;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
