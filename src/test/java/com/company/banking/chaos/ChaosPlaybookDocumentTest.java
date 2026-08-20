package com.company.banking.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Guards T-095 playbook assets so experiments stay documented and runnable locally.
 */
class ChaosPlaybookDocumentTest {

    private static final Path PLAYBOOK =
            Path.of("docs/engineering/Banking_API_Chaos_Engineering_Playbook.md");
    private static final Path COMPOSE = Path.of("docker/docker-compose.chaos.yml");
    private static final Path FAULT_SERVER = Path.of("docker/chaos/notification_fault_server.py");
    private static final Path RUNNER = Path.of("scripts/chaos/run-experiments.sh");

    @Test
    void playbookDefinesExperimentsAbortAndLocalOnlySafety() throws Exception {
        assertThat(PLAYBOOK).exists();
        String text = Files.readString(PLAYBOOK);

        assertThat(text).contains("EXP-01", "EXP-02", "EXP-03", "EXP-04");
        assertThat(text).containsIgnoringCase("hypothesis");
        assertThat(text).containsIgnoringCase("abort");
        assertThat(text).containsIgnoringCase("restore");
        assertThat(text).containsIgnoringCase("staging or production");
        assertThat(text).contains("/api/v1/health/live", "/api/v1/health/ready");
        assertThat(text).contains("notification");
    }

    @Test
    void composeOverlayAndRunnerExist() throws Exception {
        assertThat(COMPOSE).exists();
        assertThat(FAULT_SERVER).exists();
        assertThat(RUNNER).exists();

        String compose = Files.readString(COMPOSE);
        assertThat(compose).contains("notification-fault", "profiles:", "8099");

        String runner = Files.readString(RUNNER);
        assertThat(runner).contains("EXP-01", "EXP-02", "EXP-03", "EXP-04");
        assertThat(runner).contains("ABORT");
        assertThat(runner).doesNotContain("production traffic");
    }
}
