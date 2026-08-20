package com.company.banking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.banking.notification.application.NotificationPublisher;
import com.company.banking.notification.domain.NotificationMessage;
import com.company.banking.notification.infrastructure.LoggingNotificationPublisher;
import com.company.banking.notification.infrastructure.ResilientNotificationPublisher;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NotificationConfigTest {

    private final NotificationConfig config = new NotificationConfig();

    @Test
    void logProviderReturnsLoggingStub() {
        assertThat(config.loggingNotificationPublisher()).isInstanceOf(LoggingNotificationPublisher.class);
    }

    @Test
    void springDefaultProviderIsSingleLoggingStub() {
        new ApplicationContextRunner()
                .withUserConfiguration(NotificationConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationPublisher.class);
                    assertThat(context.getBean(NotificationPublisher.class))
                            .isInstanceOf(LoggingNotificationPublisher.class);
                });
    }

    @Test
    void httpProviderRequiresBaseUrl() {
        NotificationProperties properties = new NotificationProperties();
        properties.getHttp().setBaseUrl("  ");

        assertThatThrownBy(() -> config.resilientHttpNotificationPublisher(
                        properties, CircuitBreakerRegistry.ofDefaults()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOTIFICATION_HTTP_BASE_URL");
    }

    @Test
    void httpProviderWrapsWithCircuitBreakerAndNeverThrowsToCaller() {
        NotificationProperties properties = new NotificationProperties();
        properties.getHttp().setBaseUrl("http://127.0.0.1:9");
        properties.getHttp().setConnectTimeout(java.time.Duration.ofMillis(200));
        properties.getHttp().setReadTimeout(java.time.Duration.ofMillis(200));

        NotificationPublisher publisher = config.resilientHttpNotificationPublisher(
                properties, CircuitBreakerRegistry.ofDefaults());

        assertThat(publisher).isInstanceOf(ResilientNotificationPublisher.class);
        NotificationMessage message = NotificationMessage.transferCompleted(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "10.00",
                Instant.parse("2026-08-20T07:00:00Z")
        );
        assertThatCode(() -> publisher.publish(message)).doesNotThrowAnyException();
    }
}
