package com.company.banking.transaction.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.banking.support.AbstractPostgresRedisIT;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Testcontainers: Flyway V5 indexes exist and ledger history uses the composite index.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class QueryOptimizationIndexIT extends AbstractPostgresRedisIT {

    private static final int LEDGER_ROWS = 250;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("V5 query-optimization indexes are present after Flyway migrate")
    void flywayCreatesQueryOptimizationIndexes() {
        assertThat(indexDefinition("idx_transactions_account_date"))
                .contains("account_id")
                .contains("created_at");
        assertThat(indexDefinition("idx_customers_not_deleted"))
                .contains("customers")
                .contains("is_deleted");
        assertThat(indexDefinition("idx_audit_logs_actor_lower_created"))
                .contains("lower")
                .contains("created_at");
    }

    @Test
    @DisplayName("account history query uses idx_transactions_account_date (no sort)")
    void ledgerHistoryUsesCompositeIndex() {
        UUID accountId = seedAccountWithLedger(LEDGER_ROWS);
        // Other accounts so created_at-only index is a poor choice (prod-like mix).
        for (int i = 0; i < 8; i++) {
            seedAccountWithLedger(LEDGER_ROWS);
        }
        jdbcTemplate.execute("ANALYZE transactions");

        String plan = explain(
                """
                SELECT t.* FROM transactions t
                WHERE t.account_id = ?
                ORDER BY t.created_at DESC
                LIMIT 20
                """,
                accountId
        );

        assertThat(plan).contains("idx_transactions_account_date");
        assertThat(plan).doesNotContain("Sort");
    }

    private String indexDefinition(String indexName) {
        String def = jdbcTemplate.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname = ?",
                String.class,
                indexName
        );
        assertThat(def).as("index %s", indexName).isNotBlank();
        return def.toLowerCase();
    }

    private String explain(String sql, UUID accountId) {
        List<String> lines = jdbcTemplate.query(
                "EXPLAIN " + sql,
                (rs, rowNum) -> rs.getString(1),
                accountId
        );
        return String.join("\n", lines);
    }

    private UUID seedAccountWithLedger(int rowCount) {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String suffix = userId.toString().substring(0, 8);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
                userId,
                "qopt-" + suffix + "@example.com",
                "{noop}unused"
        );
        jdbcTemplate.update(
                """
                INSERT INTO customers (id, user_id, customer_number, full_name)
                VALUES (?, ?, ?, ?)
                """,
                customerId,
                userId,
                "CUST-QO-" + suffix,
                "Query Opt Seed"
        );
        jdbcTemplate.update(
                """
                INSERT INTO accounts (id, account_number, customer_id, account_type)
                VALUES (?, ?, ?, 'SAVINGS')
                """,
                accountId,
                "ACC-QO-" + suffix,
                customerId
        );

        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO transactions
                    (id, account_id, transaction_type, amount, balance_after, description, created_at)
                VALUES (?, ?, 'DEPOSIT', 1.00, 1.00, 'seed', ?)
                """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setObject(1, UUID.randomUUID());
                        ps.setObject(2, accountId);
                        ps.setTimestamp(3, Timestamp.from(base.plusSeconds(i)));
                    }

                    @Override
                    public int getBatchSize() {
                        return rowCount;
                    }
                }
        );
        return accountId;
    }
}
