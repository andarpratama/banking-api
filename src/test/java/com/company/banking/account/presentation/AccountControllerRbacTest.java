package com.company.banking.account.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * RBAC integration tests for AccountController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Freeze account (POST /api/v1/accounts/{accountId}/freeze)")
    class FreezeAccountTests {

        @Test
        @DisplayName("Admin can freeze account")
        @WithMockUser(roles = "ADMIN")
        void adminCanFreezeAccount() throws Exception {
            String accountId = "test-account-id";
            mockMvc.perform(
                    post("/api/v1/accounts/" + accountId + "/freeze")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Customer cannot freeze account (403)")
        @WithMockUser(roles = "CUSTOMER")
        void customerCannotFreezeAccount() throws Exception {
            String accountId = "test-account-id";
            mockMvc.perform(
                    post("/api/v1/accounts/" + accountId + "/freeze")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    post("/api/v1/accounts/some-id/freeze")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Close account (POST /api/v1/accounts/{accountId}/close)")
    class CloseAccountTests {

        @Test
        @DisplayName("Admin can close account")
        @WithMockUser(roles = "ADMIN")
        void adminCanCloseAccount() throws Exception {
            String accountId = "test-account-id";
            mockMvc.perform(
                    post("/api/v1/accounts/" + accountId + "/close")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Customer cannot close account (403)")
        @WithMockUser(roles = "CUSTOMER")
        void customerCannotCloseAccount() throws Exception {
            String accountId = "test-account-id";
            mockMvc.perform(
                    post("/api/v1/accounts/" + accountId + "/close")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    post("/api/v1/accounts/some-id/close")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Get account balance (GET /api/v1/accounts/{accountId}/balance)")
    class GetAccountBalanceTests {

        @Test
        @DisplayName("Admin can view any account balance")
        @WithMockUser(roles = "ADMIN")
        void adminCanViewBalance() throws Exception {
            String accountId = "test-account-id";
            mockMvc.perform(
                    get("/api/v1/accounts/" + accountId + "/balance")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Customer cannot view other customer's balance (403)")
        @WithMockUser(username = "customer1@example.com", roles = "CUSTOMER")
        void customerCannotViewOtherBalance() throws Exception {
            String accountId = "customer2-account-id";
            mockMvc.perform(
                    get("/api/v1/accounts/" + accountId + "/balance")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    get("/api/v1/accounts/some-id/balance")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Create account (POST /api/v1/accounts)")
    class CreateAccountTests {

        @Test
        @DisplayName("Customer can create account")
        @WithMockUser(roles = "CUSTOMER")
        void customerCanCreateAccount() throws Exception {
            mockMvc.perform(
                    post("/api/v1/accounts")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Admin can create account")
        @WithMockUser(roles = "ADMIN")
        void adminCanCreateAccount() throws Exception {
            mockMvc.perform(
                    post("/api/v1/accounts")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    post("/api/v1/accounts")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }
}
