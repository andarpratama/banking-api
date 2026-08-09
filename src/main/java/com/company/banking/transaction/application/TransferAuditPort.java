package com.company.banking.transaction.application;

import com.company.banking.transaction.domain.Transaction;
import java.util.UUID;

/**
 * Extension point for audit logging of transfers (filled by T-050).
 */
public interface TransferAuditPort {

    void onTransfer(UUID referenceId, Transaction debit, Transaction credit);
}
