package com.company.banking.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.company.banking.support.AbstractPostgresRedisIT;
import com.company.banking.support.AuthApiHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Validates representative v1 interactions against the live springdoc OpenAPI document.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class OpenApiSchemaValidationTest extends AbstractPostgresRedisIT {

    private static final String PASSWORD = "SecurePass123!";

    /**
     * Paths documented in {@code docs/api/Banking_API_OpenAPI_Specification.md} that springdoc must publish.
     */
    private static final List<String> CONTRACT_PATHS = List.of(
            "/api/v1/health",
            "/api/v1/health/live",
            "/api/v1/health/ready",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/customers",
            "/api/v1/customers/{customerId}",
            "/api/v1/accounts",
            "/api/v1/customers/{customerId}/accounts",
            "/api/v1/accounts/{accountId}",
            "/api/v1/accounts/{accountId}/freeze",
            "/api/v1/accounts/{accountId}/unfreeze",
            "/api/v1/accounts/{accountId}/close",
            "/api/v1/transactions/deposit",
            "/api/v1/transactions/withdraw",
            "/api/v1/transactions/transfer",
            "/api/v1/accounts/{accountId}/transactions",
            "/api/v1/accounts/{accountId}/statement",
            "/api/v1/dashboard/metrics",
            "/api/v1/audit-logs"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private OpenApiInteractionValidator validator;
    private AuthApiHelper auth;

    @DynamicPropertySource
    static void schemaValidationProperties(DynamicPropertyRegistry registry) {
        registry.add("server.compression.enabled", () -> "false");
        registry.add("app.rate-limit.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() throws Exception {
        auth = new AuthApiHelper(mockMvc, objectMapper, jdbcTemplate);
        MvcResult docs = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        validator = OpenApiInteractionSupport.validatorFromSpec(docs.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("springdoc document is OpenAPI 3 and lists markdown contract paths")
    void generatedSpecListsContractPaths() throws Exception {
        MvcResult docs = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(docs.getResponse().getContentAsString());

        assertThat(root.path("openapi").asText()).startsWith("3.");
        assertThat(root.path("info").path("title").asText()).isEqualTo("Banking API");

        JsonNode paths = root.path("paths");
        assertThat(paths.isObject()).isTrue();
        assertThat(CONTRACT_PATHS).allSatisfy(path ->
                assertThat(paths.has(path)).as("generated spec missing path %s", path).isTrue()
        );
    }

    @Test
    @DisplayName("health probes match OpenAPI schemas")
    void healthProbesMatchSchema() throws Exception {
        assertValid(mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andReturn());
        assertValid(mockMvc.perform(get("/api/v1/health/live")).andExpect(status().isOk()).andReturn());
        assertValid(mockMvc.perform(get("/api/v1/health/ready")).andExpect(status().isOk()).andReturn());
    }

    @Test
    @DisplayName("register and login success bodies match OpenAPI schemas")
    void registerAndLoginMatchSchema() throws Exception {
        String email = "oas-auth-" + System.nanoTime() + "@example.com";
        String registerBody = """
                {
                  "email": "%s",
                  "password": "%s",
                  "fullName": "Schema Tester",
                  "phone": "+15550001111",
                  "address": "1 Test Street"
                }
                """.formatted(email, PASSWORD);

        MvcResult register = mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody)
                )
                .andExpect(status().isCreated())
                .andReturn();
        assertValid(register);

        MvcResult login = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"%s","password":"%s"}
                                        """.formatted(email, PASSWORD))
                )
                .andExpect(status().isOk())
                .andReturn();
        assertValid(login);
    }

    @Test
    @DisplayName("invalid login error envelope matches OpenAPI schema")
    void loginUnauthorizedMatchesErrorSchema() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"missing@example.com","password":"WrongPass123!"}
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertValid(result);
    }

    @Test
    @DisplayName("customer, account, and deposit responses match OpenAPI schemas")
    void customerAccountDepositMatchSchema() throws Exception {
        AuthApiHelper.RegisteredUser customer = auth.register(
                "oas-flow-" + System.nanoTime() + "@example.com",
                PASSWORD,
                "Schema Flow"
        );
        String token = auth.loginAccessToken(customer.email(), PASSWORD);

        MvcResult created = mockMvc.perform(
                        post("/api/v1/accounts")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "customerId": "%s",
                                          "accountType": "SAVINGS",
                                          "currency": "USD",
                                          "initialBalance": 100.00
                                        }
                                        """.formatted(customer.customerId()))
                )
                .andExpect(status().isCreated())
                .andReturn();
        assertValid(created);

        String accountId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asText();

        MvcResult customerProfile = mockMvc.perform(
                        get("/api/v1/customers/{customerId}", customer.customerId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andReturn();
        assertValid(customerProfile);

        MvcResult account = mockMvc.perform(
                        get("/api/v1/accounts/{accountId}", accountId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andReturn();
        assertValid(account);

        MvcResult deposit = mockMvc.perform(
                        post("/api/v1/transactions/deposit")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "accountId": "%s",
                                          "amount": 25.00,
                                          "description": "Schema validation deposit"
                                        }
                                        """.formatted(accountId))
                )
                .andExpect(status().isOk())
                .andReturn();
        assertValid(deposit);
    }

    private void assertValid(MvcResult result) {
        OpenApiInteractionSupport.assertValid(validator, result);
    }
}
