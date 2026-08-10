package com.company.banking.transaction.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.banking.support.AbstractPostgresRedisIT;
import com.company.banking.support.AuthApiHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
 * Testcontainers: transfer happy path + same-account / frozen / insufficient negatives.
 * Uses unique emails per test so shared containers stay isolated.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TransferIT extends AbstractPostgresRedisIT {

    private static final String PASSWORD = "SecurePass123!";

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
    @DisplayName("transfer updates both balances and inserts DEBIT+CREDIT ledger rows")
    void transferHappyPath() throws Exception {
        Fixture fixture = createTwoAccounts("200.00", "50.00");

        MvcResult result = mockMvc.perform(
                        post("/api/v1/transactions/transfer")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sourceAccountId": "%s",
                                          "destinationAccountId": "%s",
                                          "amount": 75.00,
                                          "description": "Rent payment"
                                        }
                                        """.formatted(fixture.sourceId(), fixture.destId()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceId").isNotEmpty())
                .andExpect(jsonPath("$.sourceTransaction.transactionType").value("DEBIT"))
                .andExpect(jsonPath("$.sourceTransaction.amount").value(75.00))
                .andExpect(jsonPath("$.sourceTransaction.balanceAfter").value(125.00))
                .andExpect(jsonPath("$.sourceTransaction.description").value("Rent payment"))
                .andExpect(jsonPath("$.destinationTransaction.transactionType").value("CREDIT"))
                .andExpect(jsonPath("$.destinationTransaction.amount").value(75.00))
                .andExpect(jsonPath("$.destinationTransaction.balanceAfter").value(125.00))
                .andExpect(jsonPath("$.destinationTransaction.description").value("Rent payment"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String referenceId = body.get("referenceId").asText();
        String debitId = body.get("sourceTransaction").get("id").asText();
        String creditId = body.get("destinationTransaction").get("id").asText();

        assertThat(balanceOf(fixture.sourceId())).isEqualByComparingTo("125.00");
        assertThat(balanceOf(fixture.destId())).isEqualByComparingTo("125.00");

        List<Map<String, Object>> legs = jdbcTemplate.queryForList(
                """
                        SELECT id, account_id, transaction_type, amount, balance_after, reference_id
                        FROM transactions
                        WHERE reference_id = ?::uuid
                        ORDER BY transaction_type
                        """,
                referenceId
        );
        assertThat(legs).hasSize(2);

        Map<String, Object> credit = legs.get(0);
        Map<String, Object> debit = legs.get(1);
        assertThat(credit.get("transaction_type")).isEqualTo("CREDIT");
        assertThat(debit.get("transaction_type")).isEqualTo("DEBIT");
        assertThat(credit.get("id").toString()).isEqualTo(creditId);
        assertThat(debit.get("id").toString()).isEqualTo(debitId);
        assertThat(credit.get("account_id").toString()).isEqualTo(fixture.destId());
        assertThat(debit.get("account_id").toString()).isEqualTo(fixture.sourceId());
        assertThat(new BigDecimal(credit.get("amount").toString())).isEqualByComparingTo("75.00");
        assertThat(new BigDecimal(debit.get("amount").toString())).isEqualByComparingTo("75.00");
        assertThat(new BigDecimal(credit.get("balance_after").toString()))
                .isEqualByComparingTo("125.00");
        assertThat(new BigDecimal(debit.get("balance_after").toString()))
                .isEqualByComparingTo("125.00");
    }

    @Test
    @DisplayName("transfer to same account returns 409 SAME_ACCOUNT_TRANSFER")
    void transferRejectsSameAccount() throws Exception {
        Fixture fixture = createTwoAccounts("100.00", "0.00");

        mockMvc.perform(
                        post("/api/v1/transactions/transfer")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sourceAccountId": "%s",
                                          "destinationAccountId": "%s",
                                          "amount": 10.00
                                        }
                                        """.formatted(fixture.sourceId(), fixture.sourceId()))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SAME_ACCOUNT_TRANSFER"));

        assertThat(balanceOf(fixture.sourceId())).isEqualByComparingTo("100.00");
        assertThat(ledgerCount(fixture.sourceId())).isZero();
    }

    @Test
    @DisplayName("transfer from frozen source returns 409 ACCOUNT_FROZEN")
    void transferRejectsFrozenSource() throws Exception {
        Fixture fixture = createTwoAccounts("100.00", "0.00");

        String adminEmail = "admin-xfer-" + System.nanoTime() + "@example.com";
        auth.register(adminEmail, PASSWORD, "Admin User");
        auth.grantAdminRole(adminEmail);
        String adminToken = auth.loginAccessToken(adminEmail, PASSWORD);

        mockMvc.perform(
                        patch("/api/v1/accounts/" + fixture.sourceId() + "/freeze")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/transactions/transfer")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sourceAccountId": "%s",
                                          "destinationAccountId": "%s",
                                          "amount": 10.00
                                        }
                                        """.formatted(fixture.sourceId(), fixture.destId()))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_FROZEN"));

        assertThat(balanceOf(fixture.sourceId())).isEqualByComparingTo("100.00");
        assertThat(balanceOf(fixture.destId())).isEqualByComparingTo("0.00");
        assertThat(ledgerCount(fixture.sourceId())).isZero();
        assertThat(ledgerCount(fixture.destId())).isZero();
    }

    @Test
    @DisplayName("transfer with insufficient balance returns 409 INSUFFICIENT_BALANCE")
    void transferRejectsInsufficientBalance() throws Exception {
        Fixture fixture = createTwoAccounts("50.00", "0.00");

        mockMvc.perform(
                        post("/api/v1/transactions/transfer")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sourceAccountId": "%s",
                                          "destinationAccountId": "%s",
                                          "amount": 50.01
                                        }
                                        """.formatted(fixture.sourceId(), fixture.destId()))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));

        assertThat(balanceOf(fixture.sourceId())).isEqualByComparingTo("50.00");
        assertThat(balanceOf(fixture.destId())).isEqualByComparingTo("0.00");
        assertThat(ledgerCount(fixture.sourceId())).isZero();
    }

    private Fixture createTwoAccounts(String sourceBalance, String destBalance) throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "xfer-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Transfer Owner"
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

    private BigDecimal balanceOf(String accountId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?::uuid",
                BigDecimal.class,
                accountId
        );
    }

    private int ledgerCount(String accountId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE account_id = ?::uuid",
                Integer.class,
                accountId
        );
        return count == null ? 0 : count;
    }

    private record Fixture(String token, String sourceId, String destId) {
    }
}
