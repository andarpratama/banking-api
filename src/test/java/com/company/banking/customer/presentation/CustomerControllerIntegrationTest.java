package com.company.banking.customer.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.banking.customer.application.UpdateCustomerRequest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for Customer API endpoints.
 * Uses test profile with embedded DB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID testCustomerId = UUID.randomUUID();

    @Test
    @DisplayName("GET /customers without auth should return 401")
    void listCustomersWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /customers with CUSTOMER role should return 403")
    @WithMockUser(roles = "CUSTOMER")
    void listCustomersWithCustomerRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /customers with ADMIN role should return 200")
    @WithMockUser(roles = "ADMIN")
    void listCustomersWithAdminRoleReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /customers?page=0&size=10 with pagination params")
    @WithMockUser(roles = "ADMIN")
    void listCustomersWithPaginationParams() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "createdAt")
                .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    @DisplayName("GET /customers/{id} without auth should return 401")
    void getCustomerWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + testCustomerId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /customers/{id} with ADMIN role should return 404 for missing customer")
    @WithMockUser(roles = "ADMIN")
    void getCustomerWithAdminRoleReturns404ForMissing() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + testCustomerId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /customers/{id} with invalid request body should return 400")
    @WithMockUser(roles = "ADMIN")
    void updateCustomerWithInvalidBodyReturns400() throws Exception {
        UpdateCustomerRequest invalidRequest = new UpdateCustomerRequest("", null, null);

        mockMvc.perform(put("/api/v1/customers/" + testCustomerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /customers/{id} with CUSTOMER role (not owner) should return 403")
    @WithMockUser(username = "other@example.com", roles = "CUSTOMER")
    void updateCustomerWithoutOwnershipReturns403() throws Exception {
        UpdateCustomerRequest request = new UpdateCustomerRequest("Jane Doe", "+1-555-0456", "456 Oak Ave");

        mockMvc.perform(put("/api/v1/customers/" + testCustomerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /customers/{id} with CUSTOMER role should return 403")
    @WithMockUser(roles = "CUSTOMER")
    void deleteCustomerWithCustomerRoleReturns403() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + testCustomerId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /customers/{id} with ADMIN role returns 404 for missing customer")
    @WithMockUser(roles = "ADMIN")
    void deleteCustomerWithAdminRoleReturns404ForMissing() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + testCustomerId))
                .andExpect(status().isNotFound());
    }
}
