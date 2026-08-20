package com.company.banking.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.company.banking.notification.application.NotificationPublisher;
import com.company.banking.notification.domain.NotificationMessage;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResilientNotificationPublisherTest {

    private NotificationPublisher primary;
    private NotificationPublisher fallback;
    private CircuitBreaker circuitBreaker;
    private ResilientNotificationPublisher publisher;
    private NotificationMessage message;

    @BeforeEach
    void setUp() {
        primary = mock(NotificationPublisher.class);
        fallback = mock(NotificationPublisher.class);
        circuitBreaker = CircuitBreaker.of(
                "notification-test",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(4)
                        .minimumNumberOfCalls(4)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofMinutes(1))
                        .build()
        );
        publisher = new ResilientNotificationPublisher(circuitBreaker, primary, fallback);
        message = NotificationMessage.transferCompleted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "50.00",
                Instant.parse("2026-08-20T07:00:00Z")
        );
    }

    @Test
    void successfulPublishDoesNotCallFallback() {
        publisher.publish(message);

        verify(primary).publish(message);
        verify(fallback, never()).publish(message);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void failuresOpenCircuitThenShortCircuitToFallback() {
        doThrow(new IllegalStateException("vendor down")).when(primary).publish(message);

        for (int i = 0; i < 4; i++) {
            assertThatCode(() -> publisher.publish(message)).doesNotThrowAnyException();
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        publisher.publish(message);

        verify(primary, times(4)).publish(message);
        verify(fallback, times(5)).publish(message);
    }

    @Test
    void neverPropagatesWhenFallbackAlsoFails() {
        doThrow(new IllegalStateException("vendor down")).when(primary).publish(message);
        doThrow(new IllegalStateException("log sink down")).when(fallback).publish(message);

        assertThatCode(() -> publisher.publish(message)).doesNotThrowAnyException();
    }
}
