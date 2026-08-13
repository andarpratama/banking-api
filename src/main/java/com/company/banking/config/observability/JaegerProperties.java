package com.company.banking.config.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tracing export settings. Jaeger all-in-one and the Datadog Agent both accept OTLP
 * (the native Jaeger exporter was removed from the OpenTelemetry Java SDK).
 */
@ConfigurationProperties(prefix = "app.observability")
public class JaegerProperties {

    public static final String BACKEND_JAEGER = "jaeger";
    public static final String BACKEND_DATADOG = "datadog";

    /**
     * When false, {@link ObservabilityConfig} does not build an SDK (tests / no collector).
     */
    private boolean enabled = true;

    /**
     * OTLP gRPC endpoint, e.g. {@code http://localhost:4317} (host) or
     * {@code http://jaeger:4317} (Compose). Datadog Agent uses the same port.
     */
    private String endpoint = "http://localhost:4317";

    /**
     * Parent-based trace-id-ratio sampler argument. {@code 0.1} = 10% when there is no parent.
     */
    private double samplingRate = 0.1;

    /**
     * {@code jaeger} (local UI) or {@code datadog} (Agent / intake). Same OTLP exporter.
     */
    private String backend = BACKEND_JAEGER;

    /**
     * Resource attribute {@code deployment.environment}.
     */
    private String environment = "dev";

    /**
     * Optional Datadog intake API key ({@code dd-api-key} OTLP header). Empty when using
     * a local Datadog Agent. Never log this value.
     */
    private String datadogApiKey = "";

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

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getDatadogApiKey() {
        return datadogApiKey;
    }

    public void setDatadogApiKey(String datadogApiKey) {
        this.datadogApiKey = datadogApiKey;
    }

    public boolean isDatadogBackend() {
        return BACKEND_DATADOG.equalsIgnoreCase(backend);
    }

    public boolean hasDatadogApiKey() {
        return datadogApiKey != null && !datadogApiKey.isBlank();
    }
}
