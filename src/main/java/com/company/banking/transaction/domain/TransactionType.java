package com.company.banking.transaction.domain;

/**
 * Ledger transaction types — aligned with DB check constraint and OpenAPI.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    DEBIT,
    CREDIT
}
