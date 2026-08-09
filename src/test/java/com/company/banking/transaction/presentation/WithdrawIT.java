package com.company.banking.transaction.presentation;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Testcontainers: open account → withdraw → ledger + balance; reject insufficient funds.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class WithdrawIT extends AbstractPostgresRedisIT {

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
    @DisplayName("withdraw updates balance and inserts immutable WITHDRAW row")
    void withdrawHappyPath() throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "wd-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Withdraw Owner"
        );
        String token = auth.loginAccessToken(customer.email(), PASSWORD);
        String accountId = createAccount(token, customer.customerId(), "100.00");

        MvcResult result = mockMvc.perform(
                        post("/api/v1/transactions/withdraw")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "amount": 40.00,
                                          "description": "ATM withdrawal"
                                        }
                                        """.formatted(accountId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("WITHDRAW"))
                .andExpect(jsonPath("$.amount").value(40.00))
                .andExpect(jsonPath("$.balanceAfter").value(60.00))
                .andExpect(jsonPath("$.referenceId").isEmpty())
                .andExpect(jsonPath("$.description").value("ATM withdrawal"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String txId = body.get("id").asText();

        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM accounts WHERE id = ?::uuid",
                BigDecimal.class,
                accountId
        );
        assertThat(balance).isEqualByComparingTo("60.00");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT transaction_type, amount, balance_after, description
                        FROM transactions WHERE id = ?::uuid
                        """,
                txId
        );
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("transaction_type")).isEqualTo("WITHDRAW");
        assertThat(new BigDecimal(rows.get(0).get("amount").toString())).isEqualByComparingTo("40.00");
        assertThat(new BigDecimal(rows.get(0).get("balance_after").toString()))
                .isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("withdraw with insufficient balance returns 409 INSUFFICIENT_BALANCE")
    void withdrawRejectsInsufficientBalance() throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "wd-ins-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Insufficient Owner"
        );
        String token = auth.loginAccessToken(customer.email(), PASSWORD);
        String accountId = createAccount(token, customer.customerId(), "100.00");

        mockMvc.perform(
                        post("/api/v1/transactions/withdraw")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "amount": 300.00,
                                          "description": "ATM withdrawal"
                                        }
                                        """.formatted(accountId))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message").value(
                        "Insufficient balance. Available: 100.00, Requested: 300.00"
                ));
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
