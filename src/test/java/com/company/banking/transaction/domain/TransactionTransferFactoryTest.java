package com.company.banking.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.banking.common.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionTransferFactoryTest {

    @Test
    void debitAndCreditShareReferenceId() {
        UUID referenceId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        Money amount = Money.of(new BigDecimal("250.00"));
        Instant now = Instant.parse("2026-08-10T12:00:00Z");

        Transaction debit = Transaction.debit(
                UUID.randomUUID(),
                sourceId,
                referenceId,
                amount,
                Money.of(new BigDecimal("4750.00")),
                "Transfer to friend",
                now
        );
        Transaction credit = Transaction.credit(
                UUID.randomUUID(),
                destId,
                referenceId,
                amount,
                Money.of(new BigDecimal("250.00")),
                "Transfer to friend",
                now
        );

        assertThat(debit.transactionType()).isEqualTo(TransactionType.DEBIT);
        assertThat(credit.transactionType()).isEqualTo(TransactionType.CREDIT);
        assertThat(debit.referenceId()).isEqualTo(referenceId);
        assertThat(credit.referenceId()).isEqualTo(referenceId);
        assertThat(debit.accountId()).isEqualTo(sourceId);
        assertThat(credit.accountId()).isEqualTo(destId);
    }

    @Test
    void debitRequiresReferenceId() {
        assertThatThrownBy(() -> Transaction.debit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                Money.of(new BigDecimal("10.00")),
                Money.of(new BigDecimal("90.00")),
                null,
                Instant.now()
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void creditRequiresReferenceId() {
        assertThatThrownBy(() -> Transaction.credit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                Money.of(new BigDecimal("10.00")),
                Money.of(new BigDecimal("110.00")),
                null,
                Instant.now()
        )).isInstanceOf(NullPointerException.class);
    }
}
