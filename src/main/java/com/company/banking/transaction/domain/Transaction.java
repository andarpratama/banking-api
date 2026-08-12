package com.company.banking.transaction.domain;

import com.company.banking.common.money.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable ledger entry. Once created, never updated or deleted.
 */
public final class Transaction {

    private final UUID id;
    private final UUID accountId;
    private final UUID referenceId;
    private final TransactionType transactionType;
    private final Money amount;
    private final Money balanceAfter;
    private final String description;
    private final Instant createdAt;

    public Transaction(
            UUID id,
            UUID accountId,
            UUID referenceId,
            TransactionType transactionType,
            Money amount,
            Money balanceAfter,
            String description,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.referenceId = referenceId;
        this.transactionType = Objects.requireNonNull(transactionType, "transactionType");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.balanceAfter = Objects.requireNonNull(balanceAfter, "balanceAfter");
        this.description = description;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
    }

    public static Transaction deposit(
            UUID id,
            UUID accountId,
            Money amount,
            Money balanceAfter,
            String description,
            Instant createdAt
    ) {
        return new Transaction(
                id,
                accountId,
                null,
                TransactionType.DEPOSIT,
                amount,
                balanceAfter,
                description,
                createdAt
        );
    }

    public static Transaction withdraw(
            UUID id,
            UUID accountId,
            Money amount,
            Money balanceAfter,
            String description,
            Instant createdAt
    ) {
        return new Transaction(
                id,
                accountId,
                null,
                TransactionType.WITHDRAW,
                amount,
                balanceAfter,
                description,
                createdAt
        );
    }

    /**
     * Transfer source leg (money out). Shares {@code referenceId} with the CREDIT leg.
     */
    public static Transaction debit(
            UUID id,
            UUID accountId,
            UUID referenceId,
            Money amount,
            Money balanceAfter,
            String description,
            Instant createdAt
    ) {
        return new Transaction(
                id,
                accountId,
                Objects.requireNonNull(referenceId, "referenceId"),
                TransactionType.DEBIT,
                amount,
                balanceAfter,
                description,
                createdAt
        );
    }

    /**
     * Transfer destination leg (money in). Shares {@code referenceId} with the DEBIT leg.
     */
    public static Transaction credit(
            UUID id,
            UUID accountId,
            UUID referenceId,
            Money amount,
            Money balanceAfter,
            String description,
            Instant createdAt
    ) {
        return new Transaction(
                id,
                accountId,
                Objects.requireNonNull(referenceId, "referenceId"),
                TransactionType.CREDIT,
                amount,
                balanceAfter,
                description,
                createdAt
        );
    }

    public UUID id() {
        return id;
    }

    public UUID accountId() {
        return accountId;
    }

    public UUID referenceId() {
        return referenceId;
    }

    public TransactionType transactionType() {
        return transactionType;
    }

    public Money amount() {
        return amount;
    }

    public Money balanceAfter() {
        return balanceAfter;
    }

    public String description() {
        return description;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
