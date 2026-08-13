package com.company.banking.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
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
    }
}
