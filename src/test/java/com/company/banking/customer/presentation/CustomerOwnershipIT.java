package com.company.banking.customer.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/**
 * Testcontainers ownership: cross-customer access to customer profile is forbidden.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CustomerOwnershipIT extends AbstractPostgresRedisIT {

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
    @DisplayName("CUSTOMER cannot read or update another customer's profile (403)")
    void crossCustomerAccessIsForbidden() throws Exception {
        AuthApiHelper.RegisteredUser owner = auth.register(
                "owner-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Owner User"
        );
        AuthApiHelper.RegisteredUser other = auth.register(
                "other-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Other User"
        );

        String otherToken = auth.loginAccessToken(other.email(), PASSWORD);

        mockMvc.perform(
                        get("/api/v1/customers/" + owner.customerId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put("/api/v1/customers/" + owner.customerId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "fullName": "Hacked Name",
                                          "phone": "+15559999999",
                                          "address": "Nowhere"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CUSTOMER can read own profile after register/login")
    void ownerCanReadOwnProfile() throws Exception {
        AuthApiHelper.RegisteredUser owner = auth.register(
                "self-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Self User"
        );
        String token = auth.loginAccessToken(owner.email(), PASSWORD);

        mockMvc.perform(
                        get("/api/v1/customers/" + owner.customerId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }
}
