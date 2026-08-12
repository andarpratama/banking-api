package com.company.banking.account.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.banking.support.AbstractPostgresRedisIT;
import com.company.banking.support.AuthApiHelper;
import com.fasterxml.jackson.databind.JsonNode;
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
 * Testcontainers flow: auth → customer context → open account → freeze / unfreeze / close,
 * plus cross-customer account access forbidden.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AccountLifecycleIT extends AbstractPostgresRedisIT {

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
    @DisplayName("auth → open account → admin freeze → unfreeze → close")
    void ownershipLifecycleFreezeUnfreezeAndClose() throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "acct-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Account Owner"
        );
        String customerToken = auth.loginAccessToken(customer.email(), PASSWORD);

        String accountId = createAccount(customerToken, customer.customerId(), "100.00");

        mockMvc.perform(
                        get("/api/v1/accounts/" + accountId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(100.00));

        String adminEmail = "admin-" + System.nanoTime() + "@example.com";
        auth.register(adminEmail, PASSWORD, "Admin User");
        auth.grantAdminRole(adminEmail);
        String adminToken = auth.loginAccessToken(adminEmail, PASSWORD);

        mockMvc.perform(
                        patch("/api/v1/accounts/" + accountId + "/freeze")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        mockMvc.perform(
                        patch("/api/v1/accounts/" + accountId + "/unfreeze")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(
                        patch("/api/v1/accounts/" + accountId + "/close")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("CUSTOMER cannot open/list/view another customer's accounts (403)")
    void crossCustomerAccountAccessIsForbidden() throws Exception {
        AuthApiHelper.RegisteredUser owner = auth.register(
                "owner-acct-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Owner"
        );
        AuthApiHelper.RegisteredUser intruder = auth.register(
                "intruder-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Intruder"
        );

        String ownerToken = auth.loginAccessToken(owner.email(), PASSWORD);
        String intruderToken = auth.loginAccessToken(intruder.email(), PASSWORD);

        String accountId = createAccount(ownerToken, owner.customerId(), "25.00");

        mockMvc.perform(
                        post("/api/v1/accounts")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createAccountBody(owner.customerId(), "0"))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/v1/customers/" + owner.customerId() + "/accounts")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/v1/accounts/" + accountId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }

    private String createAccount(String accessToken, String customerId, String initialBalance)
            throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/accounts")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createAccountBody(customerId, initialBalance))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asText();
    }

    private static String createAccountBody(String customerId, String initialBalance) {
        return """
                {
                  "customerId": "%s",
                  "accountType": "SAVINGS",
                  "currency": "USD",
                  "initialBalance": %s
                }
                """.formatted(customerId, initialBalance);
    }
}
