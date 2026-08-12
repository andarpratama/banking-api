package com.company.banking.customer.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * RBAC integration tests for CustomerController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerRbacTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/v1/customers (list all)")
    class ListCustomersTests {

        @Test
        @DisplayName("Admin can list customers")
        @WithMockUser(roles = "ADMIN")
        void adminCanListCustomers() throws Exception {
            mockMvc.perform(
                    get("/api/v1/customers")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Customer cannot list all customers (403)")
        @WithMockUser(roles = "CUSTOMER")
        void customerCannotListAllCustomers() throws Exception {
            mockMvc.perform(
                    get("/api/v1/customers")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    get("/api/v1/customers")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/customers/{customerId}")
    class GetCustomerProfileTests {

        @Test
        @DisplayName("Admin is authorized to view profile (404 when customer missing)")
        @WithMockUser(roles = "ADMIN")
        void adminCanViewAnyProfile() throws Exception {
            mockMvc.perform(
                    get("/api/v1/customers/" + CUSTOMER_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Customer cannot view another profile (403)")
        @WithMockUser(username = "other@example.com", roles = "CUSTOMER")
        void customerCannotViewOtherProfile() throws Exception {
            mockMvc.perform(
                    get("/api/v1/customers/" + CUSTOMER_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    get("/api/v1/customers/" + CUSTOMER_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }
}
