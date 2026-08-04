package com.company.banking.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void ofNormalizesScaleToTwo() {
        Money money = Money.of(new BigDecimal("10.5"));

        assertThat(money.amount()).isEqualByComparingTo("10.50");
        assertThat(money.amount().scale()).isEqualTo(2);
    }

    @Test
    void ofPositiveAcceptsMinimumIncrement() {
        Money money = Money.ofPositive(new BigDecimal("0.01"));

        assertThat(money.isPositive()).isTrue();
        assertThat(money.amount()).isEqualByComparingTo("0.01");
    }

    @Test
    void ofPositiveRejectsZero() {
        assertThatThrownBy(() -> Money.ofPositive(BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException business = (BusinessException) ex;
                    assertThat(business.getErrorCode()).isEqualTo(ErrorCode.INVALID_AMOUNT);
                    assertThat(business.getMessage()).contains("greater than zero");
                });
    }

    @Test
    void ofPositiveRejectsNegative() {
        assertThatThrownBy(() -> Money.ofPositive(new BigDecimal("-1.00")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_AMOUNT));
    }

    @Test
    void ofRejectsScaleGreaterThanTwo() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("10.001")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException business = (BusinessException) ex;
                    assertThat(business.getErrorCode()).isEqualTo(ErrorCode.INVALID_AMOUNT);
                    assertThat(business.getMessage()).contains("decimal places");
                });
    }

    @Test
    void ofNonNegativeAllowsZero() {
        Money money = Money.ofNonNegative(BigDecimal.ZERO);

        assertThat(money.isZero()).isTrue();
        assertThat(money.amount()).isEqualByComparingTo("0.00");
    }

    @Test
    void ofNonNegativeRejectsNegative() {
        assertThatThrownBy(() -> Money.ofNonNegative(new BigDecimal("-0.01")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_AMOUNT));
    }

    @Test
    void ofRejectsNull() {
        assertThatThrownBy(() -> Money.of(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_AMOUNT));
    }

    @Test
    void addAndSubtractPreserveScale() {
        Money left = Money.of(new BigDecimal("10.00"));
        Money right = Money.of(new BigDecimal("2.50"));

        assertThat(left.add(right).amount()).isEqualByComparingTo("12.50");
        assertThat(left.subtract(right).amount()).isEqualByComparingTo("7.50");
    }

    @Test
    void equalsComparesAmountValue() {
        assertThat(Money.of(new BigDecimal("1.10")))
                .isEqualTo(Money.of(new BigDecimal("1.1")));
    }
}
