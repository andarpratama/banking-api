package com.company.banking.transaction.domain;

/**
 * Transaction repository port — insert-only ledger.
 */
public interface TransactionRepository {

    /**
     * Persists a new immutable ledger row. Must not update existing rows.
     */
    Transaction save(Transaction transaction);
}
