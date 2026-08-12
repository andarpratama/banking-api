package com.company.banking.transaction.application;

import com.company.banking.transaction.domain.Transaction;

/**
 * Extension point for audit logging of deposits (filled by T-050).
 */
public interface DepositAuditPort {

    void onDeposit(Transaction transaction);
}
