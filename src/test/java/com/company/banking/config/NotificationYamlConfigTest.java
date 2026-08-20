package com.company.banking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards notification + Resilience4j defaults in {@code application.yml} (T-094).
 */
class NotificationYamlConfigTest {

    @Test
    @SuppressWarnings("unchecked")
    void applicationYmlDefaultsToLogProviderAndNotificationCircuitBreaker() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertThat(in).as("application.yml on classpath").isNotNull();

            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> app = (Map<String, Object>) root.get("app");
            Map<String, Object> notification = (Map<String, Object>) app.get("notification");

            assertThat(notification.get("provider")).isEqualTo("${NOTIFICATION_PROVIDER:log}");
            Map<String, Object> http = (Map<String, Object>) notification.get("http");
            assertThat(http.get("base-url")).isEqualTo("${NOTIFICATION_HTTP_BASE_URL:}");

            Map<String, Object> resilience = (Map<String, Object>) root.get("resilience4j");
            Map<String, Object> circuitbreaker = (Map<String, Object>) resilience.get("circuitbreaker");
            Map<String, Object> instances = (Map<String, Object>) circuitbreaker.get("instances");
            assertThat(instances).containsKey("notification");

            Map<String, Object> management = (Map<String, Object>) root.get("management");
            Map<String, Object> health = (Map<String, Object>) management.get("health");
            Map<String, Object> circuitbreakers = (Map<String, Object>) health.get("circuitbreakers");
            assertThat(circuitbreakers.get("enabled")).isEqualTo(false);
        }
    }
}
