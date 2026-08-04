package com.company.banking.common.money;

import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable monetary amount using {@link BigDecimal} with fixed scale 2
 * (aligned with {@code DECIMAL(19,2)}). Never use {@code double}/{@code float} for money.
 */
public final class Money implements Comparable<Money> {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.UNNECESSARY;

    private static final BigDecimal MIN_POSITIVE = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999999999999.99");

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Creates money from a non-null amount with at most {@value #SCALE} decimal places.
     * Zero and negative values are allowed (use {@link #ofPositive} / {@link #ofNonNegative} when needed).
     */
    public static Money of(BigDecimal amount) {
        return new Money(normalize(amount));
    }

    /**
     * Creates money that must be strictly greater than zero (transaction amounts).
     */
    public static Money ofPositive(BigDecimal amount) {
        Money money = of(amount);
        if (money.amount.compareTo(MIN_POSITIVE) < 0) {
            throw invalidAmount("Amount must be greater than zero");
        }
        return money;
    }

    /**
     * Creates money that must be greater than or equal to zero (balances).
     */
    public static Money ofNonNegative(BigDecimal amount) {
        Money money = of(amount);
        if (money.amount.compareTo(BigDecimal.ZERO) < 0) {
            throw invalidAmount("Amount must not be negative");
        }
        return money;
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO.setScale(SCALE));
    }

    public BigDecimal amount() {
        return amount;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "other");
        return of(amount.add(other.amount));
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "other");
        return of(amount.subtract(other.amount));
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }

    private static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            throw invalidAmount("Amount is required");
        }
        if (amount.scale() > SCALE) {
            throw invalidAmount("Amount must have at most " + SCALE + " decimal places");
        }
        BigDecimal scaled = amount.setScale(SCALE, ROUNDING_MODE);
        if (scaled.abs().compareTo(MAX_AMOUNT) > 0) {
            throw invalidAmount("Amount exceeds maximum allowed value");
        }
        return scaled;
    }

    private static BusinessException invalidAmount(String message) {
        return new BusinessException(ErrorCode.INVALID_AMOUNT, message);
    }
}
