package com.company.banking.dashboard.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * RBAC tests for DashboardController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Admin can view dashboard metrics")
    @WithMockUser(roles = "ADMIN")
    void adminCanViewDashboard() throws Exception {
        mockMvc.perform(
                get("/api/v1/dashboard/metrics")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
        )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Customer cannot view dashboard metrics (403)")
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotViewDashboard() throws Exception {
        mockMvc.perform(
                get("/api/v1/dashboard/metrics")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
        )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user gets 401")
    void unauthenticatedUserGets401() throws Exception {
        mockMvc.perform(
                get("/api/v1/dashboard/metrics")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized());
    }
}
