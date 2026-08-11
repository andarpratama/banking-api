package com.company.banking.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.company.banking.notification.domain.NotificationMessage;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoggingNotificationPublisherTest {

    @Test
    void publishDoesNotThrow() {
        LoggingNotificationPublisher publisher = new LoggingNotificationPublisher();
        NotificationMessage message = NotificationMessage.transferCompleted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "50.00",
                Instant.parse("2026-08-11T07:00:00Z")
        );

        assertThatCode(() -> publisher.publish(message)).doesNotThrowAnyException();
    }
}
