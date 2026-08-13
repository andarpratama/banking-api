package com.company.banking.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class OpenTelemetryFactoryTest {

    @Test
    void createRecordsSpanWhenSamplingIsAlwaysOn() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        JaegerProperties properties = new JaegerProperties();
        properties.setSamplingRate(1.0d);

        try (OpenTelemetrySdk sdk = OpenTelemetryFactory.create(properties, exporter)) {
            Tracer tracer = sdk.getTracer("test");
            Span span = tracer.spanBuilder("deposit").startSpan();
            span.end();
            assertThat(sdk.getSdkTracerProvider().forceFlush().join(5, TimeUnit.SECONDS).isSuccess()).isTrue();

            assertThat(exporter.getFinishedSpanItems())
                    .extracting(SpanData::getName)
                    .containsExactly("deposit");
        }
    }

    @Test
    void clampRatioBoundsSamplingRate() {
        assertThat(OpenTelemetryFactory.clampRatio(-1.0d)).isZero();
        assertThat(OpenTelemetryFactory.clampRatio(0.1d)).isEqualTo(0.1d);
        assertThat(OpenTelemetryFactory.clampRatio(2.0d)).isEqualTo(1.0d);
    }

    @Test
    void defaultPropertiesPointAtLocalJaegerOtlp() {
        JaegerProperties properties = new JaegerProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getEndpoint()).isEqualTo("http://localhost:4317");
        assertThat(properties.getSamplingRate()).isEqualTo(0.1d);
        assertThat(properties.getBackend()).isEqualTo(JaegerProperties.BACKEND_JAEGER);
        assertThat(properties.isDatadogBackend()).isFalse();
        assertThat(properties.hasDatadogApiKey()).isFalse();
    }

    @Test
    void otlpHeadersIncludeDatadogApiKeyOnlyForDatadogBackend() {
        JaegerProperties jaeger = new JaegerProperties();
        jaeger.setDatadogApiKey("dd-secret");
        assertThat(OpenTelemetryFactory.otlpHeaders(jaeger)).isEmpty();

        JaegerProperties datadogNoKey = new JaegerProperties();
        datadogNoKey.setBackend(JaegerProperties.BACKEND_DATADOG);
        assertThat(OpenTelemetryFactory.otlpHeaders(datadogNoKey)).isEmpty();

        JaegerProperties datadog = new JaegerProperties();
        datadog.setBackend("DATADOG");
        datadog.setDatadogApiKey("dd-secret");
        assertThat(OpenTelemetryFactory.otlpHeaders(datadog))
                .isEqualTo(Map.of(OpenTelemetryFactory.DATADOG_API_KEY_HEADER, "dd-secret"));
    }

    @Test
    void resourceIncludesDeploymentEnvironment() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        JaegerProperties properties = new JaegerProperties();
        properties.setSamplingRate(1.0d);
        properties.setEnvironment("staging");

        try (OpenTelemetrySdk sdk = OpenTelemetryFactory.create(properties, exporter)) {
            Tracer tracer = sdk.getTracer("test");
            tracer.spanBuilder("login").startSpan().end();
            assertThat(sdk.getSdkTracerProvider().forceFlush().join(5, TimeUnit.SECONDS).isSuccess()).isTrue();

            SpanData span = exporter.getFinishedSpanItems().getFirst();
            assertThat(span.getResource().getAttribute(AttributeKey.stringKey("service.name")))
                    .isEqualTo(OpenTelemetryFactory.SERVICE_NAME);
            assertThat(span.getResource().getAttribute(AttributeKey.stringKey("deployment.environment")))
                    .isEqualTo("staging");
        }
    }

    @Test
    void blankEnvironmentDefaultsToDev() {
        JaegerProperties properties = new JaegerProperties();
        properties.setEnvironment("  ");
        assertThat(OpenTelemetryFactory.environmentOrDefault(properties)).isEqualTo("dev");
    }
}
