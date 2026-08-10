package com.company.banking.transaction.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.banking.support.AbstractPostgresRedisIT;
import com.company.banking.support.AuthApiHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
 * Testcontainers: deposit/withdraw then statement for date range; reject non-owner.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AccountStatementIT extends AbstractPostgresRedisIT {

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
    @DisplayName("owner gets statement with balances and totals for period")
    void statementHappyPath() throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "stmt-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Statement Owner"
        );
        String token = auth.loginAccessToken(customer.email(), PASSWORD);
        String accountId = createAccount(token, customer.customerId(), "0.00");

        deposit(token, accountId, "1500.00", "Cash deposit");
        withdraw(token, accountId, "300.00", "ATM cash");

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        mockMvc.perform(
                        get("/api/v1/accounts/" + accountId + "/statement")
                                .param("fromDate", today.toString())
                                .param("toDate", today.toString())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.openingBalance").value(0.00))
                .andExpect(jsonPath("$.closingBalance").value(1200.00))
                .andExpect(jsonPath("$.totalDeposits").value(1500.00))
                .andExpect(jsonPath("$.totalWithdrawals").value(300.00))
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.transactions[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.transactions[1].type").value("WITHDRAW"));
    }

    @Test
    @DisplayName("non-owner receives 403 when requesting another account statement")
    void statementRejectsNonOwner() throws Exception {
        AuthApiHelper.RegisteredUser owner = auth.register(
                "stmt-own-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Owner"
        );
        String ownerToken = auth.loginAccessToken(owner.email(), PASSWORD);
        String accountId = createAccount(ownerToken, owner.customerId(), "100.00");

        AuthApiHelper.RegisteredUser other = auth.register(
                "stmt-oth-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Other"
        );
        String otherToken = auth.loginAccessToken(other.email(), PASSWORD);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        mockMvc.perform(
                        get("/api/v1/accounts/" + accountId + "/statement")
                                .param("fromDate", today.toString())
                                .param("toDate", today.toString())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }

    private void deposit(String token, String accountId, String amount, String description)
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/transactions/deposit")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "amount": %s,
                                          "description": "%s"
                                        }
                                        """.formatted(accountId, amount, description))
                )
                .andExpect(status().isOk());
    }

    private void withdraw(String token, String accountId, String amount, String description)
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/transactions/withdraw")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "amount": %s,
                                          "description": "%s"
                                        }
                                        """.formatted(accountId, amount, description))
                )
                .andExpect(status().isOk());
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
