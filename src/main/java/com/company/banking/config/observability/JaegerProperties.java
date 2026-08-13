package com.company.banking.config.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tracing export settings. Jaeger all-in-one accepts OTLP (native Jaeger exporter
 * was removed from the OpenTelemetry Java SDK).
 */
@ConfigurationProperties(prefix = "app.observability")
public class JaegerProperties {

    /**
     * When false, {@link ObservabilityConfig} does not build an SDK (tests / no collector).
     */
    private boolean enabled = true;

    /**
     * OTLP gRPC endpoint, e.g. {@code http://localhost:4317} (host) or
     * {@code http://jaeger:4317} (Compose).
     */
    private String endpoint = "http://localhost:4317";

    /**
     * Parent-based trace-id-ratio sampler argument. {@code 0.1} = 10% when there is no parent.
     */
    private double samplingRate = 0.1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public double getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(double samplingRate) {
        this.samplingRate = samplingRate;
    }
}
