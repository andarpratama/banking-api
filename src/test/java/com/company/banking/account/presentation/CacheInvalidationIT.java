package com.company.banking.account.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Cached customer/account reads must reflect updates and money movements immediately.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CacheInvalidationIT extends AbstractPostgresRedisIT {

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
    @DisplayName("GET customer after PUT returns the updated profile (not a stale cache hit)")
    void customerUpdateIsVisibleOnCachedGet() throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "cache-cust-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Original Name"
        );
        String token = auth.loginAccessToken(customer.email(), PASSWORD);

        mockMvc.perform(
                        get("/api/v1/customers/" + customer.customerId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Original Name"));

        mockMvc.perform(
                        put("/api/v1/customers/" + customer.customerId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fullName": "Updated Name",
                                          "phone": "+15550009999",
                                          "address": "99 Cache Lane"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));

        mockMvc.perform(
                        get("/api/v1/customers/" + customer.customerId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.phone").value("+15550009999"));
    }

    @Test
    @DisplayName("GET account after deposit returns the new balance (not a stale cache hit)")
    void depositIsVisibleOnCachedAccountGet() throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "cache-acct-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Cache Owner"
        );
        String token = auth.loginAccessToken(customer.email(), PASSWORD);
        String accountId = createAccount(token, customer.customerId(), "100.00");

        mockMvc.perform(
                        get("/api/v1/accounts/" + accountId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));

        mockMvc.perform(
                        post("/api/v1/transactions/deposit")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "amount": 50.00,
                                          "description": "Prime then credit"
                                        }
                                        """.formatted(accountId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(150.00));

        mockMvc.perform(
                        get("/api/v1/accounts/" + accountId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
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
