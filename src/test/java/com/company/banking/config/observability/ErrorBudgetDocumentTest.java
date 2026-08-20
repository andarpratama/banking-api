package com.company.banking.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Guards T-097 error-budget recordings, MWMB alerts, and local Alertmanager wiring.
 */
class ErrorBudgetDocumentTest {

    private static final Path ERROR_BUDGET_DOC =
            Path.of("docs/engineering/Banking_API_Error_Budget.md");
    private static final Path PROMETHEUS_CONFIG = Path.of("docker/observability/prometheus.yml");
    private static final Path RECORDING_RULES =
            Path.of("docker/observability/prometheus/rules/error-budget-recording-rules.yml");
    private static final Path ALERT_RULES =
            Path.of("docker/observability/prometheus/rules/error-budget-alerts.yml");
    private static final Path SLO_RECORDING_RULES =
            Path.of("docker/observability/prometheus/rules/slo-recording-rules.yml");
    private static final Path ALERTMANAGER = Path.of("docker/observability/alertmanager.yml");
    private static final Path COMPOSE = Path.of("docker/docker-compose.dev.yml");
    private static final Path APPLICATION_YML = Path.of("src/main/resources/application.yml");

    @Test
    void errorBudgetDocumentDefinesSreModelAndMwmb() throws Exception {
        assertThat(ERROR_BUDGET_DOC).exists();
        String text = Files.readString(ERROR_BUDGET_DOC);

        assertThat(text).contains("Google SRE");
        assertThat(text).containsIgnoringCase("error budget");
        assertThat(text).containsIgnoringCase("burn rate");
        assertThat(text).contains("14.4");
        assertThat(text).contains("MWMB");
        assertThat(text).contains("not a customer SLA");
        assertThat(text).contains("no paging");
        assertThat(text).contains("24 h");
        assertThat(text).contains("30-day");
        assertThat(text).contains("le=\"0.3\"");
    }

    @Test
    void prometheusLoadsErrorBudgetRulesAndAlertmanager() throws Exception {
        assertThat(PROMETHEUS_CONFIG).exists();
        assertThat(RECORDING_RULES).exists();
        assertThat(ALERT_RULES).exists();
        assertThat(ALERTMANAGER).exists();
        assertThat(COMPOSE).exists();

        String prometheus = Files.readString(PROMETHEUS_CONFIG);
        assertThat(prometheus).contains("/etc/prometheus/rules/error-budget-recording-rules.yml");
        assertThat(prometheus).contains("/etc/prometheus/rules/error-budget-alerts.yml");
        assertThat(prometheus).contains("alertmanagers:");
        assertThat(prometheus).contains("alertmanager:9093");

        String recordings = Files.readString(RECORDING_RULES);
        assertThat(recordings).contains("record: banking:error_budget:availability:remaining24h");
        assertThat(recordings).contains("record: banking:error_budget:availability:burn5m");
        assertThat(recordings).contains("record: banking:error_budget:availability:burn1h");
        assertThat(recordings).contains("record: banking:error_budget:availability:hours_to_exhaustion");
        assertThat(recordings).contains("record: banking:error_budget:latency:remaining24h");
        assertThat(recordings).contains("le=\"0.3\"");
        assertThat(recordings).doesNotContain("alert:");

        String alerts = Files.readString(ALERT_RULES);
        assertThat(alerts).contains("alert: BankingApiErrorBudgetFastBurn");
        assertThat(alerts).contains("alert: BankingApiErrorBudgetSlowBurn");
        assertThat(alerts).contains("alert: BankingApiErrorBudgetTicket");
        assertThat(alerts).contains("14.4");
        assertThat(alerts).contains("severity: page");
        assertThat(alerts).contains("severity: ticket");
        assertThat(alerts).contains("and");

        String sloRecordings = Files.readString(SLO_RECORDING_RULES);
        assertThat(sloRecordings).doesNotContain("ALERT");
        assertThat(sloRecordings).doesNotContain("alert:");

        String alertmanager = Files.readString(ALERTMANAGER);
        assertThat(alertmanager).contains("receiver: local-dev");
        assertThat(alertmanager).doesNotContain("pagerduty");
        assertThat(alertmanager).doesNotContain("smtp_");
        assertThat(alertmanager).doesNotContain("webhook_configs");

        String compose = Files.readString(COMPOSE);
        assertThat(compose).contains("alertmanager:");
        assertThat(compose).contains("9093:9093");
        assertThat(compose).contains("./observability/alertmanager.yml");
        assertThat(compose).contains("prom/alertmanager:");
    }

    @Test
    void httpHistogramExposesThreeHundredMillisecondBucket() throws Exception {
        String yaml = Files.readString(APPLICATION_YML);
        assertThat(yaml).contains("300ms");
        assertThat(yaml).contains("slo:");
        assertThat(yaml).contains("[http.server.requests]");
    }
}
