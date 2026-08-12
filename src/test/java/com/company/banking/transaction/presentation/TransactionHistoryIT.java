package com.company.banking.transaction.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.banking.support.AbstractPostgresRedisIT;
import com.company.banking.support.AuthApiHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Testcontainers: deposit then list history with type filter; reject non-owner.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TransactionHistoryIT extends AbstractPostgresRedisIT {

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
    @DisplayName("owner lists history with type filter and pagination")
    void historyHappyPathWithFilter() throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "hist-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "History Owner"
        );
        String token = auth.loginAccessToken(customer.email(), PASSWORD);
        String accountId = createAccount(token, customer.customerId(), "100.00");

        deposit(token, accountId, "50.00", "Cash deposit");
        withdraw(token, accountId, "20.00", "ATM cash");

        mockMvc.perform(
                        get("/api/v1/accounts/" + accountId + "/transactions")
                                .param("type", "DEPOSIT")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "createdAt,desc")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[0].amount").value(50.00))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    @DisplayName("non-owner receives 403 when listing another account history")
    void historyRejectsNonOwner() throws Exception {
        AuthApiHelper.RegisteredUser owner = auth.register(
                "hist-own-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Owner"
        );
        String ownerToken = auth.loginAccessToken(owner.email(), PASSWORD);
        String accountId = createAccount(ownerToken, owner.customerId(), "100.00");
        deposit(ownerToken, accountId, "10.00", null);

        AuthApiHelper.RegisteredUser other = auth.register(
                "hist-oth-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Other"
        );
        String otherToken = auth.loginAccessToken(other.email(), PASSWORD);

        mockMvc.perform(
                        get("/api/v1/accounts/" + accountId + "/transactions")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }

    private void deposit(String token, String accountId, String amount, String description)
            throws Exception {
        String descJson = description == null ? "" : ", \"description\": \"" + description + "\"";
        mockMvc.perform(
                        post("/api/v1/transactions/deposit")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "amount": %s%s
                                        }
                                        """.formatted(accountId, amount, descJson))
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
