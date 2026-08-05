package com.company.banking.customer.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * RBAC integration tests for CustomerController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerRbacTest {

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
        @DisplayName("Admin can view any customer profile")
        @WithMockUser(roles = "ADMIN")
        void adminCanViewAnyProfile() throws Exception {
            String customerId = "test-customer-id";
            mockMvc.perform(
                    get("/api/v1/customers/" + customerId)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Customer viewing own profile succeeds (pending implementation)")
        @WithMockUser(username = "test-customer-id", roles = "CUSTOMER")
        void customerCanViewOwnProfile() throws Exception {
            // Note: The actual ownership check is deferred to application layer
            // For now, this tests that the endpoint doesn't reject based on role alone
            mockMvc.perform(
                    get("/api/v1/customers/test-customer-id")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Unauthenticated user gets 401")
        void unauthenticatedUserGets401() throws Exception {
            mockMvc.perform(
                    get("/api/v1/customers/some-id")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(csrf())
            )
                    .andExpect(status().isUnauthorized());
        }
    }
}
