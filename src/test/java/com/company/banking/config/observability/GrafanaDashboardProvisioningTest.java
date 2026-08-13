package com.company.banking.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GrafanaDashboardProvisioningTest {

    private static final Path DASHBOARDS = Path.of("docker/observability/grafana/dashboards");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void latencyAndErrorRateDashboardsAreProvisioned() throws Exception {
        Path latency = DASHBOARDS.resolve("http-latency.json");
        Path errorRate = DASHBOARDS.resolve("http-error-rate.json");
        assertThat(latency).exists();
        assertThat(errorRate).exists();

        JsonNode latencyJson = MAPPER.readTree(Files.readString(latency));
        assertThat(latencyJson.path("title").asText()).isEqualTo("HTTP Latency");
        assertThat(latencyJson.path("uid").asText()).isEqualTo("banking-http-latency");
        assertThat(latencyJson.path("panels").isArray()).isTrue();
        assertThat(latencyJson.path("panels")).hasSizeGreaterThanOrEqualTo(2);
        assertThat(panelTitles(latencyJson)).anyMatch(title -> title.toLowerCase().contains("p99"));
        assertThat(panelExprs(latencyJson)).anyMatch(expr -> expr.contains("histogram_quantile(0.99"));

        JsonNode errorJson = MAPPER.readTree(Files.readString(errorRate));
        assertThat(errorJson.path("title").asText()).isEqualTo("HTTP Error Rate");
        assertThat(errorJson.path("uid").asText()).isEqualTo("banking-http-error-rate");
        assertThat(errorJson.path("panels").isArray()).isTrue();
        assertThat(errorJson.path("panels")).hasSizeGreaterThanOrEqualTo(2);
        assertThat(panelTitles(errorJson)).anyMatch(title -> title.toLowerCase().contains("error rate"));
        assertThat(panelExprs(errorJson)).anyMatch(expr -> expr.contains("status=~\"5..\""));
    }

    private static Stream<String> panelTitles(JsonNode dashboard) {
        return dashboard.path("panels").findValuesAsText("title").stream();
    }

    private static Stream<String> panelExprs(JsonNode dashboard) {
        return dashboard.path("panels").findValuesAsText("expr").stream();
    }
}
