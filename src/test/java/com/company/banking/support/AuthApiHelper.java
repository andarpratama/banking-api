package com.company.banking.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Small helpers for register/login and promoting a user to ADMIN in IT suites.
 */
public final class AuthApiHelper {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public AuthApiHelper(MockMvc mockMvc, ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public RegisteredUser register(String email, String password, String fullName) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s",
                  "fullName": "%s",
                  "phone": "+15550001111",
                  "address": "1 Test Street"
                }
                """.formatted(email, password, fullName);

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new RegisteredUser(
                json.get("id").asText(),
                json.get("email").asText(),
                json.get("customerId").asText()
        );
    }

    public String loginAccessToken(String email, String password) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    /**
     * Grants ADMIN role in DB; caller must re-login to receive ADMIN in the JWT.
     */
    public void grantAdminRole(String email) {
        jdbcTemplate.update(
                """
                        INSERT INTO user_roles (user_id, role_id)
                        SELECT u.id, r.id
                        FROM users u
                        CROSS JOIN roles r
                        WHERE lower(u.email) = lower(?)
                          AND r.name = 'ADMIN'
                        ON CONFLICT DO NOTHING
                        """,
                email
        );
    }

    public record RegisteredUser(String userId, String email, String customerId) {
    }
}
