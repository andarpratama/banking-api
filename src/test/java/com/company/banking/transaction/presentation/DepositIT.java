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
 * Testcontainers: open account → deposit → ledger + balance; reject frozen.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class DepositIT extends AbstractPostgresRedisIT {

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
    @DisplayName("deposit updates balance and inserts immutable DEPOSIT row")
    void depositHappyPath() throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "dep-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Deposit Owner"
        );
        String token = auth.loginAccessToken(customer.email(), PASSWORD);
        String accountId = createAccount(token, customer.customerId(), "100.00");

        MvcResult result = mockMvc.perform(
                        post("/api/v1/transactions/deposit")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "amount": 50.00,
                                          "description": "Cash deposit at ATM"
                                        }
                                        """.formatted(accountId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.balanceAfter").value(150.00))
                .andExpect(jsonPath("$.referenceId").isEmpty())
                .andExpect(jsonPath("$.description").value("Cash deposit at ATM"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String txId = body.get("id").asText();

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?::uuid",
                BigDecimal.class,
                accountId
        );
        assertThat(balance).isEqualByComparingTo("150.00");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT transaction_type, amount, balance_after, description
                        FROM transactions WHERE id = ?::uuid
                        """,
                txId
        );
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("transaction_type")).isEqualTo("DEPOSIT");
        assertThat(new BigDecimal(rows.get(0).get("amount").toString())).isEqualByComparingTo("50.00");
        assertThat(new BigDecimal(rows.get(0).get("balance_after").toString()))
                .isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("deposit on frozen account returns 409 ACCOUNT_FROZEN")
    void depositRejectsFrozen() throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "dep-frz-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Frozen Owner"
        );
        String customerToken = auth.loginAccessToken(customer.email(), PASSWORD);
        String accountId = createAccount(customerToken, customer.customerId(), "100.00");

        String adminEmail = "admin-dep-" + System.nanoTime() + "@example.com";
        auth.register(adminEmail, PASSWORD, "Admin User");
        auth.grantAdminRole(adminEmail);
        String adminToken = auth.loginAccessToken(adminEmail, PASSWORD);

        mockMvc.perform(
                        patch("/api/v1/accounts/" + accountId + "/freeze")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/transactions/deposit")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "amount": 10.00
                                        }
                                        """.formatted(accountId))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_FROZEN"));
    }

    @Test
    @DisplayName("deposit with zero amount returns 400 INVALID_AMOUNT")
    void depositRejectsInvalidAmount() throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "dep-amt-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Amount Owner"
        );
        String token = auth.loginAccessToken(customer.email(), PASSWORD);
        String accountId = createAccount(token, customer.customerId(), "100.00");

        mockMvc.perform(
                        post("/api/v1/transactions/deposit")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "amount": 0
                                        }
                                        """.formatted(accountId))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AMOUNT"));
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
}
