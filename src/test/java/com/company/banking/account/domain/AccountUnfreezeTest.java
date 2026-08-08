package com.company.banking.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.common.money.Money;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for FROZEN → ACTIVE unfreeze transitions.
 */
class AccountUnfreezeTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-09T10:00:00Z");

    @Test
    @DisplayName("unfreeze transitions FROZEN → ACTIVE")
    void unfreezeTransitionsFrozenToActive() {
        Account frozen = activeAccount().freeze(NOW);
        Instant later = NOW.plusSeconds(90);

        Account unfrozen = frozen.unfreeze(later);

        assertThat(unfrozen).isNotSameAs(frozen);
        assertThat(unfrozen.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(unfrozen.isActive()).isTrue();
        assertThat(unfrozen.updatedAt()).isEqualTo(later);
        assertThat(frozen.status()).isEqualTo(AccountStatus.FROZEN);
    }

    @Test
    @DisplayName("unfreeze rejects ACTIVE (not frozen)")
    void unfreezeRejectsActive() {
        Account active = activeAccount();

        assertThatThrownBy(() -> active.unfreeze(NOW.plusSeconds(1)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(be.getMessage()).containsIgnoringCase("not frozen");
                });
    }

    @Test
    @DisplayName("unfreeze rejects CLOSED")
    void unfreezeRejectsClosed() {
        Account closed = activeAccount().close(NOW);

        assertThatThrownBy(() -> closed.unfreeze(NOW.plusSeconds(1)))
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
                "USD",
                Money.zero(),
                NOW
        );
    }
}
