package com.company.banking.config.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Assembles an {@link OpenTelemetrySdk} with W3C Trace Context and parent-based sampling.
 * Callers supply the span exporter (OTLP in production, in-memory in tests).
 */
final class OpenTelemetryFactory {

    static final String SERVICE_NAME = "banking-api";
    static final String DATADOG_API_KEY_HEADER = "dd-api-key";

    private OpenTelemetryFactory() {
    }

    static OpenTelemetrySdk create(JaegerProperties properties) {
        return create(properties, otlpSpanExporter(properties));
    }

    static OpenTelemetrySdk create(JaegerProperties properties, SpanExporter spanExporter) {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), SERVICE_NAME,
                        AttributeKey.stringKey("deployment.environment"),
                        environmentOrDefault(properties))));

        SpanProcessor processor = BatchSpanProcessor.builder(spanExporter)
                .setScheduleDelay(Duration.ofSeconds(5))
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(processor)
                .setResource(resource)
                .setSampler(Sampler.parentBased(Sampler.traceIdRatioBased(clampRatio(properties.getSamplingRate()))))
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }

    static SpanExporter otlpSpanExporter(JaegerProperties properties) {
        OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
                .setEndpoint(properties.getEndpoint());
        otlpHeaders(properties).forEach(builder::addHeader);
        return builder.build();
    }

    /**
     * Extra OTLP headers. Datadog intake needs {@code dd-api-key}; a local Agent does not.
     * Values are not logged.
     */
    static Map<String, String> otlpHeaders(JaegerProperties properties) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (properties.isDatadogBackend() && properties.hasDatadogApiKey()) {
            headers.put(DATADOG_API_KEY_HEADER, properties.getDatadogApiKey());
        }
        return Map.copyOf(headers);
    }

    static String environmentOrDefault(JaegerProperties properties) {
        String environment = properties.getEnvironment();
        if (environment == null || environment.isBlank()) {
            return "dev";
        }
        return environment;
    }

    static double clampRatio(double samplingRate) {
        if (samplingRate < 0.0d) {
            return 0.0d;
        }
        if (samplingRate > 1.0d) {
            return 1.0d;
        }
        return samplingRate;
    }
}
