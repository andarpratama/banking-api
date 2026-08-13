package com.company.banking.config.observability;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the OpenTelemetry SDK (OTLP → Jaeger) used by the Spring Boot starter
 * auto-instrumentation (HTTP, JDBC). Disabled when {@code otel.sdk.disabled=true}
 * so Testcontainers / unit tests do not export spans.
 */
@Configuration
@EnableConfigurationProperties(JaegerProperties.class)
public class ObservabilityConfig {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityConfig.class);

    private final JaegerProperties jaegerProperties;

    public ObservabilityConfig(JaegerProperties jaegerProperties) {
        this.jaegerProperties = jaegerProperties;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "otel.sdk.disabled", havingValue = "false", matchIfMissing = true)
    public OpenTelemetrySdk openTelemetrySdk() {
        if (!jaegerProperties.isEnabled()) {
            log.info("OpenTelemetry SDK skipped (app.observability.enabled=false)");
            return OpenTelemetrySdk.builder().build();
        }
        OpenTelemetrySdk sdk = OpenTelemetryFactory.create(jaegerProperties);
        log.info(
                "OpenTelemetry initialized with OTLP exporter (Jaeger): endpoint={} samplingRate={}",
                jaegerProperties.getEndpoint(),
                jaegerProperties.getSamplingRate()
        );
        return sdk;
    }
}
