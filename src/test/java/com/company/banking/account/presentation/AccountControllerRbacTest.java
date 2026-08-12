package com.company.banking.account.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
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
 * RBAC integration tests for AccountController (OpenAPI-aligned paths).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerRbacTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Freeze account (PATCH /api/v1/accounts/{accountId}/freeze)")
    class FreezeAccountTests {

        @Test
        @DisplayName("Admin is authorized to freeze (404 when account missing)")
        @WithMockUser(roles = "ADMIN")
        void adminCanFreezeAccount() throws Exception {
            mockMvc.perform(
                    patch("/api/v1/accounts/" + ACCOUNT_ID + "/freeze")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Customer cannot freeze account (403)")
        @WithMockUser(roles = "CUSTOMER")
        void customerCannotFreezeAccount() throws Exception {
            mockMvc.perform(
                    patch("/api/v1/accounts/" + ACCOUNT_ID + "/freeze")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    patch("/api/v1/accounts/" + ACCOUNT_ID + "/freeze")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Unfreeze account (PATCH /api/v1/accounts/{accountId}/unfreeze)")
    class UnfreezeAccountTests {

        @Test
        @DisplayName("Admin is authorized to unfreeze (404 when account missing)")
        @WithMockUser(roles = "ADMIN")
        void adminCanUnfreezeAccount() throws Exception {
            mockMvc.perform(
                    patch("/api/v1/accounts/" + ACCOUNT_ID + "/unfreeze")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Customer cannot unfreeze account (403)")
        @WithMockUser(roles = "CUSTOMER")
        void customerCannotUnfreezeAccount() throws Exception {
            mockMvc.perform(
                    patch("/api/v1/accounts/" + ACCOUNT_ID + "/unfreeze")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    patch("/api/v1/accounts/" + ACCOUNT_ID + "/unfreeze")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Close account (PATCH /api/v1/accounts/{accountId}/close)")
    class CloseAccountTests {

        @Test
        @DisplayName("Admin is authorized to close (404 when account missing)")
        @WithMockUser(roles = "ADMIN")
        void adminCanCloseAccount() throws Exception {
            mockMvc.perform(
                    patch("/api/v1/accounts/" + ACCOUNT_ID + "/close")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Customer cannot close account (403)")
        @WithMockUser(roles = "CUSTOMER")
        void customerCannotCloseAccount() throws Exception {
            mockMvc.perform(
                    patch("/api/v1/accounts/" + ACCOUNT_ID + "/close")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    patch("/api/v1/accounts/" + ACCOUNT_ID + "/close")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Get account (GET /api/v1/accounts/{accountId})")
    class GetAccountTests {

        @Test
        @DisplayName("Admin is authorized to view account (404 when missing)")
        @WithMockUser(roles = "ADMIN")
        void adminCanViewAccount() throws Exception {
            mockMvc.perform(
                    get("/api/v1/accounts/" + ACCOUNT_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Customer is allowed past role check for get (404 when missing)")
        @WithMockUser(username = "customer1@example.com", roles = "CUSTOMER")
        void customerCanCallGetAccount() throws Exception {
            mockMvc.perform(
                    get("/api/v1/accounts/" + ACCOUNT_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    get("/api/v1/accounts/" + ACCOUNT_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Create account (POST /api/v1/accounts)")
    class CreateAccountTests {

        private String createBody() {
            return """
                    {
                      "customerId": "%s",
                      "accountType": "SAVINGS",
                      "currency": "USD",
                      "initialBalance": 0
                    }
                    """.formatted(CUSTOMER_ID);
        }

        @Test
        @DisplayName("Customer role reaches create but cannot create for non-owned customer (403)")
        @WithMockUser(roles = "CUSTOMER")
        void customerCanCreateAccount() throws Exception {
            mockMvc.perform(
                    post("/api/v1/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin role can call create (404 when customer missing)")
        @WithMockUser(roles = "ADMIN")
        void adminCanCreateAccount() throws Exception {
            mockMvc.perform(
                    post("/api/v1/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    post("/api/v1/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createBody())
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }
}
