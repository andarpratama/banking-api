package com.company.banking.dashboard.application;

/**
 * OpenAPI daily/weekly block: deposits, withdrawals, transfers.
 */
public class PeriodVolumeResponse {

    private VolumeResponse deposits;
    private VolumeResponse withdrawals;
    private VolumeResponse transfers;

    public PeriodVolumeResponse() {
    }

    public PeriodVolumeResponse(
            VolumeResponse deposits,
            VolumeResponse withdrawals,
            VolumeResponse transfers
    ) {
        this.deposits = deposits;
        this.withdrawals = withdrawals;
        this.transfers = transfers;
    }

    public VolumeResponse getDeposits() {
        return deposits;
    }

    public void setDeposits(VolumeResponse deposits) {
        this.deposits = deposits;
    }

    public VolumeResponse getWithdrawals() {
        return withdrawals;
    }

    public void setWithdrawals(VolumeResponse withdrawals) {
        this.withdrawals = withdrawals;
    }

    public VolumeResponse getTransfers() {
        return transfers;
    }

    public void setTransfers(VolumeResponse transfers) {
        this.transfers = transfers;
    }
}
