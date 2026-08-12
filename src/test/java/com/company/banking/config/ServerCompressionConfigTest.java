package com.company.banking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards gzip compression defaults in {@code application.yml} (quick-win: API response compression).
 */
class ServerCompressionConfigTest {

    @Test
    @SuppressWarnings("unchecked")
    void applicationYmlEnablesGzipCompressionForJsonResponses() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertThat(in).as("application.yml on classpath").isNotNull();

            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> server = (Map<String, Object>) root.get("server");
            Map<String, Object> compression = (Map<String, Object>) server.get("compression");

            assertThat(compression.get("enabled")).isEqualTo(true);
            assertThat(compression.get("min-response-size")).isEqualTo(1024);
            assertThat((List<String>) compression.get("mime-types"))
                    .contains("application/json", "application/xml", "text/plain");
        }
    }
}
