package com.company.banking.transaction.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.banking.support.AbstractPostgresRedisIT;
import com.company.banking.support.AuthApiHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Concurrent transfer integration tests: optimistic locking must prevent lost updates.
 * Conflicts surface as 409 {@code OPTIMISTIC_LOCK_EXCEPTION}; clients may retry.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ConcurrentTransferIT extends AbstractPostgresRedisIT {

    private static final String PASSWORD = "SecurePass123!";
    private static final int THREAD_COUNT = 20;
    private static final String TRANSFER_AMOUNT = "10.00";
    private static final int MAX_RETRIES = 40;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AuthApiHelper auth;

    @BeforeEach
    void setUp() {
        auth = new AuthApiHelper(mockMvc, objectMapper, jdbcTemplate);
    }

    @Test
    @DisplayName("parallel transfers with client retry yield expected balances (no lost updates)")
    void concurrentTransfersWithRetryPreserveExpectedBalances() throws Exception {
        Fixture fixture = createFixture("1000.00", "0.00");

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>(THREAD_COUNT);

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    transferWithRetry(fixture.token(), fixture.sourceId(), fixture.destId());
                    return true;
                }));
            }
            start.countDown();
            for (Future<Boolean> future : futures) {
                assertThat(future.get(60, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            pool.shutdownNow();
        }

        BigDecimal sourceBalance = balanceOf(fixture.sourceId());
        BigDecimal destBalance = balanceOf(fixture.destId());
        BigDecimal expectedMoved = new BigDecimal(TRANSFER_AMOUNT).multiply(BigDecimal.valueOf(THREAD_COUNT));

        assertThat(sourceBalance).isEqualByComparingTo(new BigDecimal("1000.00").subtract(expectedMoved));
        assertThat(destBalance).isEqualByComparingTo(expectedMoved);
        assertThat(sourceBalance.add(destBalance)).isEqualByComparingTo("1000.00");
        assertThat(successfulTransferCount(fixture.sourceId())).isEqualTo(THREAD_COUNT);
    }

    @Test
    @DisplayName("concurrent conflicts return OPTIMISTIC_LOCK_EXCEPTION; money is conserved")
    void concurrentConflictsAreDeterministicAndConserveMoney() throws Exception {
        Fixture fixture = createFixture("1000.00", "0.00");

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger optimisticLocks = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>(THREAD_COUNT);

        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    MvcResult result = mockMvc.perform(
                                    post("/api/v1/transactions/transfer")
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(transferBody(fixture.sourceId(), fixture.destId()))
                            )
                            .andReturn();
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        successes.incrementAndGet();
                    } else if (status == 409) {
                        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
                        String code = body.path("code").asText();
                        if ("OPTIMISTIC_LOCK_EXCEPTION".equals(code)) {
                            optimisticLocks.incrementAndGet();
                        } else {
                            unexpected.incrementAndGet();
                        }
                    } else {
                        unexpected.incrementAndGet();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(unexpected.get()).isZero();
        assertThat(successes.get() + optimisticLocks.get()).isEqualTo(THREAD_COUNT);
        assertThat(successes.get()).isGreaterThan(0);
        // Under contention at least one request should lose the version race.
        assertThat(optimisticLocks.get()).isGreaterThan(0);

        BigDecimal sourceBalance = balanceOf(fixture.sourceId());
        BigDecimal destBalance = balanceOf(fixture.destId());
        BigDecimal moved = new BigDecimal(TRANSFER_AMOUNT).multiply(BigDecimal.valueOf(successes.get()));

        assertThat(sourceBalance.add(destBalance)).isEqualByComparingTo("1000.00");
        assertThat(destBalance).isEqualByComparingTo(moved);
        assertThat(sourceBalance).isEqualByComparingTo(new BigDecimal("1000.00").subtract(moved));
        assertThat(successfulTransferCount(fixture.sourceId())).isEqualTo(successes.get());
    }

    private void transferWithRetry(String token, String sourceId, String destId) throws Exception {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            MvcResult result = mockMvc.perform(
                            post("/api/v1/transactions/transfer")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(transferBody(sourceId, destId))
                    )
                    .andReturn();
            int status = result.getResponse().getStatus();
            if (status == 200) {
                return;
            }
            if (status == 409) {
                JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
                assertThat(body.path("code").asText()).isEqualTo("OPTIMISTIC_LOCK_EXCEPTION");
                Thread.onSpinWait();
                continue;
            }
            throw new AssertionError(
                    "Unexpected transfer status " + status + ": " + result.getResponse().getContentAsString()
            );
        }
        throw new AssertionError("Transfer did not succeed after " + MAX_RETRIES + " retries");
    }

    private Fixture createFixture(String sourceBalance, String destBalance) throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "ctx-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Concurrent Owner"
        );
        String token = auth.loginAccessToken(customer.email(), PASSWORD);
        String sourceId = createAccount(token, customer.customerId(), sourceBalance);
        String destId = createAccount(token, customer.customerId(), destBalance);
        return new Fixture(token, sourceId, destId);
    }

    private String createAccount(String accessToken, String customerId, String initialBalance)
            throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/accounts")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "customerId": "%s",
                                          "accountType": "SAVINGS",
                                          "currency": "USD",
                                          "initialBalance": %s
                                        }
                                        """.formatted(customerId, initialBalance))
                )
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText();
    }

    private String transferBody(String sourceId, String destId) {
        return """
                {
                  "sourceAccountId": "%s",
                  "destinationAccountId": "%s",
                  "amount": %s,
                  "description": "Concurrent transfer"
                }
                """.formatted(sourceId, destId, TRANSFER_AMOUNT);
    }

    private BigDecimal balanceOf(String accountId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?::uuid",
                BigDecimal.class,
                accountId
        );
    }

    private int successfulTransferCount(String sourceAccountId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM transactions
                        WHERE account_id = ?::uuid AND transaction_type = 'DEBIT'
                        """,
                Integer.class,
                sourceAccountId
        );
        return count == null ? 0 : count;
    }

    private record Fixture(String token, String sourceId, String destId) {
    }
}
