package com.company.banking.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.common.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-08T10:00:00Z");

    @Test
    void createStartsActiveWithZeroVersion() {
        Account account = Account.create(
                ID,
                CUSTOMER_ID,
                "ACC-0000001",
                AccountType.SAVINGS,
                "USD",
                Money.of(new BigDecimal("100.00")),
                NOW
        );

        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.version()).isZero();
        assertThat(account.balance().amount()).isEqualByComparingTo("100.00");
        assertThat(account.isActive()).isTrue();
    }

    @Test
    void freezeTransitionsActiveToFrozen() {
        Account active = activeAccount();
        Instant later = NOW.plusSeconds(60);

        Account frozen = active.freeze(later);

        assertThat(frozen).isNotSameAs(active);
        assertThat(frozen.status()).isEqualTo(AccountStatus.FROZEN);
        assertThat(frozen.isFrozen()).isTrue();
        assertThat(frozen.updatedAt()).isEqualTo(later);
        assertThat(active.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void freezeRejectsAlreadyFrozen() {
        Account frozen = activeAccount().freeze(NOW);

        assertThatThrownBy(() -> frozen.freeze(NOW.plusSeconds(1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_FROZEN);
                });
    }

    @Test
    void freezeRejectsClosed() {
        Account closed = activeAccount().close(NOW);

        assertThatThrownBy(() -> closed.freeze(NOW.plusSeconds(1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_CLOSED);
                });
    }

    @Test
    void closeTransitionsActiveToClosed() {
        Account active = activeAccount();
        Instant later = NOW.plusSeconds(120);

        Account closed = active.close(later);

        assertThat(closed.status()).isEqualTo(AccountStatus.CLOSED);
        assertThat(closed.isClosed()).isTrue();
        assertThat(closed.updatedAt()).isEqualTo(later);
    }

    @Test
    void closeRejectsAlreadyClosed() {
        Account closed = activeAccount().close(NOW);

        assertThatThrownBy(() -> closed.close(NOW.plusSeconds(1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_CLOSED);
                });
    }

    @Test
    void closeRejectsFrozen() {
        Account frozen = activeAccount().freeze(NOW);

        assertThatThrownBy(() -> frozen.close(NOW.plusSeconds(1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_FROZEN);
                    assertThat(be.getMessage()).containsIgnoringCase("unfreeze");
                });
    }

    @Test
    void rejectsUnsupportedCurrency() {
        assertThatThrownBy(() ->
                Account.create(
                        ID,
                        CUSTOMER_ID,
                        "ACC-0000001",
                        AccountType.CHECKING,
                        "JPY",
                        Money.zero(),
                        NOW
                )
        )
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                });
    }

    @Test
    void creditIncreasesBalanceOnActiveAccount() {
        Account active = activeAccount();
        Instant later = NOW.plusSeconds(30);
        Money deposit = Money.ofPositive(new BigDecimal("250.50"));

        Account credited = active.credit(deposit, later);

        assertThat(credited).isNotSameAs(active);
        assertThat(credited.balance().amount()).isEqualByComparingTo("250.50");
        assertThat(credited.updatedAt()).isEqualTo(later);
        assertThat(active.balance().isZero()).isTrue();
    }

    @Test
    void creditRejectsFrozenAccount() {
        Account frozen = activeAccount().freeze(NOW);

        assertThatThrownBy(() -> frozen.credit(Money.ofPositive(new BigDecimal("10.00")), NOW.plusSeconds(1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_FROZEN);
                    assertThat(be.getMessage()).isEqualTo("Cannot transact on frozen account");
                });
    }

    @Test
    void creditRejectsClosedAccount() {
        Account closed = activeAccount().close(NOW);

        assertThatThrownBy(() -> closed.credit(Money.ofPositive(new BigDecimal("10.00")), NOW.plusSeconds(1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_CLOSED);
                    assertThat(be.getMessage()).isEqualTo("Cannot transact on closed account");
                });
    }

    @Test
    void debitDecreasesBalanceOnActiveAccount() {
        Account funded = activeAccount().credit(Money.ofPositive(new BigDecimal("100.00")), NOW);
        Instant later = NOW.plusSeconds(30);
        Money withdrawal = Money.ofPositive(new BigDecimal("40.00"));

        Account debited = funded.debit(withdrawal, later);

        assertThat(debited).isNotSameAs(funded);
        assertThat(debited.balance().amount()).isEqualByComparingTo("60.00");
        assertThat(debited.updatedAt()).isEqualTo(later);
        assertThat(funded.balance().amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void debitAllowsExactBalanceBoundary() {
        Account funded = activeAccount().credit(Money.ofPositive(new BigDecimal("100.00")), NOW);

        Account emptied = funded.debit(Money.ofPositive(new BigDecimal("100.00")), NOW.plusSeconds(1));

        assertThat(emptied.balance().isZero()).isTrue();
    }

    @Test
    void debitRejectsInsufficientBalanceByOneCent() {
        Account funded = activeAccount().credit(Money.ofPositive(new BigDecimal("100.00")), NOW);

        assertThatThrownBy(() -> funded.debit(Money.ofPositive(new BigDecimal("100.01")), NOW.plusSeconds(1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
                    assertThat(be.getMessage()).isEqualTo(
                            "Insufficient balance. Available: 100.00, Requested: 100.01"
                    );
                });
    }

    @Test
    void debitRejectsFrozenAccount() {
        Account frozen = activeAccount()
                .credit(Money.ofPositive(new BigDecimal("50.00")), NOW)
                .freeze(NOW.plusSeconds(1));

        assertThatThrownBy(() -> frozen.debit(Money.ofPositive(new BigDecimal("10.00")), NOW.plusSeconds(2)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_FROZEN);
                });
    }

    @Test
    void debitRejectsClosedAccount() {
        Account closed = activeAccount()
                .credit(Money.ofPositive(new BigDecimal("50.00")), NOW)
                .close(NOW.plusSeconds(1));

        assertThatThrownBy(() -> closed.debit(Money.ofPositive(new BigDecimal("10.00")), NOW.plusSeconds(2)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_CLOSED);
                });
    }

    private Account activeAccount() {
        return Account.create(
                ID,
                CUSTOMER_ID,
                "ACC-0000001",
                AccountType.SAVINGS,
                "usd",
                Money.zero(),
                NOW
        );
    }
}
