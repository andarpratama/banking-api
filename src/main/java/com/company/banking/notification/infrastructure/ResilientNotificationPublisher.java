package com.company.banking.notification.infrastructure;

import com.company.banking.notification.application.NotificationPublisher;
import com.company.banking.notification.domain.NotificationMessage;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorates a primary {@link NotificationPublisher} with a circuit breaker. Failures and an open
 * circuit fall back to the logging stub so money/auth flows never fail on vendor outages.
 */
public final class ResilientNotificationPublisher implements NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(ResilientNotificationPublisher.class);

    private final CircuitBreaker circuitBreaker;
    private final NotificationPublisher primary;
    private final NotificationPublisher fallback;

    public ResilientNotificationPublisher(
            CircuitBreaker circuitBreaker,
            NotificationPublisher primary,
            NotificationPublisher fallback
    ) {
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker, "circuitBreaker");
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    @Override
    public void publish(NotificationMessage message) {
        Objects.requireNonNull(message, "message");
        try {
            circuitBreaker.executeRunnable(() -> primary.publish(message));
        } catch (CallNotPermittedException ex) {
            log.debug(
                    "Notification circuit open; using fallback. state={} type={} recipientId={}",
                    circuitBreaker.getState(),
                    message.type(),
                    message.recipientId()
            );
            safeFallback(message);
        } catch (RuntimeException ex) {
            log.warn(
                    "Notification primary failed; using fallback. state={} type={} recipientId={}",
                    circuitBreaker.getState(),
                    message.type(),
                    message.recipientId(),
                    ex
            );
            safeFallback(message);
        }
    }

    private void safeFallback(NotificationMessage message) {
        try {
            fallback.publish(message);
        } catch (RuntimeException ex) {
            log.warn(
                    "Notification fallback failed (ignored): type={} recipientId={}",
                    message.type(),
                    message.recipientId(),
                    ex
            );
        }
    }
}
