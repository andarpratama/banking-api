package com.company.banking.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationMessageTest {

    private final Instant now = Instant.parse("2026-08-11T07:00:00Z");
    private final UUID recipientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID referenceId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void transferCompletedBuildsExpectedPayload() {
        NotificationMessage message = NotificationMessage.transferCompleted(
                recipientId,
                referenceId,
                "100.00",
                now
        );

        assertThat(message.recipientId()).isEqualTo(recipientId);
        assertThat(message.type()).isEqualTo(NotificationType.TRANSFER_COMPLETED);
        assertThat(message.title()).isEqualTo("Transfer completed");
        assertThat(message.body()).isEqualTo(
                "Transfer 22222222-2222-2222-2222-222222222222 for amount 100.00 completed"
        );
        assertThat(message.occurredAt()).isEqualTo(now);
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> new NotificationMessage(
                recipientId,
                NotificationType.DEPOSIT_COMPLETED,
                "  ",
                "body",
                now
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }
}
