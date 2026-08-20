package com.company.banking.notification.infrastructure;

import com.company.banking.notification.application.NotificationPublisher;
import com.company.banking.notification.domain.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging / no-op notification adapter until a real email/SMS/push vendor is plugged in.
 * Never propagates failures to callers so money/auth transactions stay authoritative.
 * Default {@code app.notification.provider=log}; also used as circuit-breaker fallback (T-094).
 */
public class LoggingNotificationPublisher implements NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationPublisher.class);

    @Override
    public void publish(NotificationMessage message) {
        try {
            log.info(
                    "Notification stub: type={} recipientId={} title={} body={} occurredAt={}",
                    message.type(),
                    message.recipientId(),
                    message.title(),
                    message.body(),
                    message.occurredAt()
            );
        } catch (RuntimeException ex) {
            log.warn(
                    "Notification stub failed (ignored): type={} recipientId={}",
                    message != null ? message.type() : null,
                    message != null ? message.recipientId() : null,
                    ex
            );
        }
    }
}
