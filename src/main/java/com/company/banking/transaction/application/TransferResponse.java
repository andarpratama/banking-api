package com.company.banking.transaction.application;

import java.util.UUID;

/**
 * Transfer result with shared reference and dual ledger legs (OpenAPI §4.3).
 */
public class TransferResponse {

    private UUID referenceId;
    private TransactionResponse sourceTransaction;
    private TransactionResponse destinationTransaction;

    public TransferResponse() {
    }

    public TransferResponse(
            UUID referenceId,
            TransactionResponse sourceTransaction,
            TransactionResponse destinationTransaction
    ) {
        this.referenceId = referenceId;
        this.sourceTransaction = sourceTransaction;
        this.destinationTransaction = destinationTransaction;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public TransactionResponse getSourceTransaction() {
        return sourceTransaction;
    }

    public void setSourceTransaction(TransactionResponse sourceTransaction) {
        this.sourceTransaction = sourceTransaction;
    }

    public TransactionResponse getDestinationTransaction() {
        return destinationTransaction;
    }

    public void setDestinationTransaction(TransactionResponse destinationTransaction) {
        this.destinationTransaction = destinationTransaction;
    }
}
