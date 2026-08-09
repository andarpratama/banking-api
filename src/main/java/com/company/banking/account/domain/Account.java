package com.company.banking.account.domain;

import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.common.money.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Account domain entity — balance + status with optimistic locking version.
 * Immutable; state transitions return a new instance.
 */
public final class Account {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP");

    private final UUID id;
    private final UUID customerId;
    private final String accountNumber;
    private final AccountType accountType;
    private final String currency;
    private final Money balance;
    private final AccountStatus status;
    private final long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Account(
            UUID id,
            UUID customerId,
            String accountNumber,
            AccountType accountType,
            String currency,
            Money balance,
            AccountStatus status,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.accountType = Objects.requireNonNull(accountType, "accountType");
        this.currency = requireCurrency(currency);
        this.balance = Objects.requireNonNull(balance, "balance");
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * Creates a new ACTIVE account with version 0.
     */
    public static Account create(
            UUID id,
            UUID customerId,
            String accountNumber,
            AccountType accountType,
            String currency,
            Money initialBalance,
            Instant now
    ) {
        return new Account(
                id,
                customerId,
                accountNumber,
                accountType,
                currency,
                initialBalance,
                AccountStatus.ACTIVE,
                0L,
                now,
                now
        );
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public String accountNumber() {
        return accountNumber;
    }

    public AccountType accountType() {
        return accountType;
    }

    public String currency() {
        return currency;
    }

    public Money balance() {
        return balance;
    }

    public AccountStatus status() {
        return status;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean isFrozen() {
        return status == AccountStatus.FROZEN;
    }

    public boolean isClosed() {
        return status == AccountStatus.CLOSED;
    }

    /**
     * Credits the account balance (deposit). Account must be ACTIVE.
     */
    public Account credit(Money amount, Instant now) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(now, "now");
        requireActiveForTransaction();
        if (!amount.isPositive()) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "Amount must be greater than zero");
        }
        return new Account(
                this.id,
                this.customerId,
                this.accountNumber,
                this.accountType,
                this.currency,
                this.balance.add(amount),
                this.status,
                this.version,
                this.createdAt,
                now
        );
    }

    /**
     * ACTIVE → FROZEN.
     */
    public Account freeze(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status == AccountStatus.FROZEN) {
            throw new BusinessException(ErrorCode.ACCOUNT_FROZEN, "Account is already frozen");
        }
        if (status == AccountStatus.CLOSED) {
            throw new BusinessException(ErrorCode.ACCOUNT_CLOSED, "Cannot freeze a closed account");
        }
        return withStatus(AccountStatus.FROZEN, now);
    }

    /**
     * ACTIVE → CLOSED (final). FROZEN accounts must be unfrozen first (T-033).
     */
    public Account close(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status == AccountStatus.CLOSED) {
            throw new BusinessException(ErrorCode.ACCOUNT_CLOSED, "Account is already closed");
        }
        if (status == AccountStatus.FROZEN) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_FROZEN,
                    "Cannot close a frozen account; unfreeze first"
            );
        }
        return withStatus(AccountStatus.CLOSED, now);
    }

    private Account withStatus(AccountStatus newStatus, Instant now) {
        return new Account(
                this.id,
                this.customerId,
                this.accountNumber,
                this.accountType,
                this.currency,
                this.balance,
                newStatus,
                this.version,
                this.createdAt,
                now
        );
    }

    private void requireActiveForTransaction() {
        if (status == AccountStatus.FROZEN) {
            throw new BusinessException(ErrorCode.ACCOUNT_FROZEN, "Cannot transact on frozen account");
        }
        if (status == AccountStatus.CLOSED) {
            throw new BusinessException(ErrorCode.ACCOUNT_CLOSED, "Cannot transact on closed account");
        }
    }

    private static String requireCurrency(String currency) {
        Objects.requireNonNull(currency, "currency");
        String normalized = currency.trim().toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(normalized)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Unsupported currency: " + currency + ". Allowed: USD, EUR, GBP"
            );
        }
        return normalized;
    }
}
