package com.company.banking.audit.presentation;

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
 * RBAC tests for AuditController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditControllerRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Admin can list audit logs")
    @WithMockUser(roles = "ADMIN")
    void adminCanListAuditLogs() throws Exception {
        mockMvc.perform(
                get("/api/v1/audit-logs")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
        )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Customer cannot list audit logs (403)")
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotListAuditLogs() throws Exception {
        mockMvc.perform(
                get("/api/v1/audit-logs")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
        )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user gets 401")
    void unauthenticatedUserGets401() throws Exception {
        mockMvc.perform(
                get("/api/v1/audit-logs")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized());
    }
}
