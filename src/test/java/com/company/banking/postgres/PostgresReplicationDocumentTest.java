package com.company.banking.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Guards T-101 local primary/replica streaming-replication assets.
 */
class PostgresReplicationDocumentTest {

    private static final Path DOC =
            Path.of("docs/engineering/Banking_API_Postgres_Replication.md");
    private static final Path COMPOSE = Path.of("docker/docker-compose.replication.yml");
    private static final Path PG_HBA = Path.of("docker/postgres/replication/pg_hba.conf");
    private static final Path INIT = Path.of("docker/postgres/replication/00-replication.sh");
    private static final Path REPLICA_ENTRYPOINT =
            Path.of("docker/postgres/replication/replica-entrypoint.sh");
    private static final Path VERIFY = Path.of("scripts/postgres/verify-replication.sh");
    private static final Path STATEFULSET = Path.of("k8s/base/postgres-statefulset.yaml");

    @Test
    void documentDefinesAsyncTopologyAndOutOfScope() throws Exception {
        assertThat(DOC).exists();
        String text = Files.readString(DOC);

        assertThat(text).containsIgnoringCase("async");
        assertThat(text).containsIgnoringCase("streaming replication");
        assertThat(text).contains("region-a", "region-b");
        assertThat(text).contains("5432", "5433");
        assertThat(text).contains("T-102", "T-103", "T-104");
        assertThat(text).contains("pg_stat_replication");
        assertThat(text).contains("wal_level");
        assertThat(text).contains("hot_standby");
        assertThat(text).containsIgnoringCase("not a backup");
        assertThat(text).contains("spec.replicas: 1");
        assertThat(text).containsIgnoringCase("staging or production");
    }

    @Test
    void overlayAndScriptsExist() throws Exception {
        assertThat(COMPOSE).exists();
        assertThat(PG_HBA).exists();
        assertThat(INIT).exists();
        assertThat(REPLICA_ENTRYPOINT).exists();
        assertThat(VERIFY).exists();

        String compose = Files.readString(COMPOSE);
        assertThat(compose).contains("profiles:", "replication");
        assertThat(compose).contains("postgres-replica");
        assertThat(compose).contains("5433:5432");
        assertThat(compose).contains("wal_level=replica");
        assertThat(compose).contains("region_b_slot");
        assertThat(compose).contains("banking.region: region-a");
        assertThat(compose).contains("banking.region: region-b");

        String hba = Files.readString(PG_HBA);
        assertThat(hba).contains("replication");
        assertThat(hba).contains("replicator");

        String entrypoint = Files.readString(REPLICA_ENTRYPOINT);
        assertThat(entrypoint).contains("pg_basebackup");
        assertThat(entrypoint).contains("standby.signal");
        assertThat(entrypoint).contains("hot_standby=on");

        String verify = Files.readString(VERIFY);
        assertThat(verify).contains("pg_is_in_recovery");
        assertThat(verify).contains("pg_stat_replication");
        assertThat(verify).contains("pg_replication_slots");
        assertThat(verify).contains("region_b_slot");
        assertThat(verify).doesNotContain("production traffic");
    }

    @Test
    void kubernetesPostgresStaysSinglePrimary() throws Exception {
        assertThat(STATEFULSET).exists();
        String yaml = Files.readString(STATEFULSET);
        assertThat(yaml).contains("replicas: 1");
        assertThat(yaml).contains("streaming replication");
    }
}
