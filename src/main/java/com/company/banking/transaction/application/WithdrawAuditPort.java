package com.company.banking.transaction.application;

import com.company.banking.transaction.domain.Transaction;

/**
 * Extension point for audit logging of withdrawals (filled by T-050).
 */
public interface WithdrawAuditPort {

    void onWithdraw(Transaction transaction);
}
