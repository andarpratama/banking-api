package com.company.banking.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Guards T-096 SLO catalog, PromQL recording rules, and Grafana wiring.
 */
class SloDefinitionDocumentTest {

    private static final Path SLO_DOC = Path.of("docs/engineering/Banking_API_SLO.md");
    private static final Path PROMETHEUS_CONFIG = Path.of("docker/observability/prometheus.yml");
    private static final Path RECORDING_RULES =
            Path.of("docker/observability/prometheus/rules/slo-recording-rules.yml");
    private static final Path COMPOSE = Path.of("docker/docker-compose.dev.yml");

    @Test
    void sloDocumentDefinesAvailabilityLatencyAndErrorRate() throws Exception {
        assertThat(SLO_DOC).exists();
        String text = Files.readString(SLO_DOC);

        assertThat(text).contains("SLO-01", "SLO-02", "SLO-03");
        assertThat(text).contains("99.9%");
        assertThat(text).containsIgnoringCase("availability");
        assertThat(text).containsIgnoringCase("error rate");
        assertThat(text).containsIgnoringCase("p99");
        assertThat(text).contains("300 ms");
        assertThat(text).contains("not a customer SLA");
        assertThat(text).contains("T-097");
        assertThat(text).contains("uri!~\"/api/v1/health.*\"");
        assertThat(text).contains("status!~\"4..\"");
    }

    @Test
    void prometheusLoadsSloRecordingRules() throws Exception {
        assertThat(PROMETHEUS_CONFIG).exists();
        assertThat(RECORDING_RULES).exists();
        assertThat(COMPOSE).exists();

        String prometheus = Files.readString(PROMETHEUS_CONFIG);
        assertThat(prometheus).contains("rule_files:");
        assertThat(prometheus).contains("/etc/prometheus/rules/slo-recording-rules.yml");

        String rules = Files.readString(RECORDING_RULES);
        assertThat(rules).contains("record: banking:sli:availability:ratio5m");
        assertThat(rules).contains("record: banking:sli:error_rate:ratio5m");
        assertThat(rules).contains("record: banking:sli:latency_p99:seconds5m");
        assertThat(rules).contains("record: banking:sli:latency_p99_money:seconds5m");
        assertThat(rules).contains("histogram_quantile(0.99");
        assertThat(rules).contains("status=~\"5..\"");
        assertThat(rules).doesNotContain("ALERT");

        String compose = Files.readString(COMPOSE);
        assertThat(compose).contains("./observability/prometheus/rules:/etc/prometheus/rules:ro");
    }
}
