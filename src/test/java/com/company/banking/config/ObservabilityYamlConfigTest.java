package com.company.banking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards OpenTelemetry / Jaeger defaults in {@code application.yml} (T-092).
 */
class ObservabilityYamlConfigTest {

    @Test
    @SuppressWarnings("unchecked")
    void applicationYmlEnablesOtlpTracingToJaegerWithTenPercentSampling() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertThat(in).as("application.yml on classpath").isNotNull();

            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> app = (Map<String, Object>) root.get("app");
            Map<String, Object> observability = (Map<String, Object>) app.get("observability");
            Map<String, Object> management = (Map<String, Object>) root.get("management");
            Map<String, Object> tracing = (Map<String, Object>) management.get("tracing");
            Map<String, Object> sampling = (Map<String, Object>) tracing.get("sampling");

            assertThat(observability.get("endpoint").toString()).contains("4317");
            assertThat(observability.get("sampling-rate").toString()).contains("0.1");
            assertThat(sampling.get("probability").toString()).contains("0.1");
        }
    }
}
