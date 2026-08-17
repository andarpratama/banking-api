package com.company.banking.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.company.banking.support.AbstractPostgresRedisIT;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies checked-in consumer contracts against a live Spring Boot provider.
 * JWT placeholders are replaced after {@link PactStatesProvider} seeds data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("banking-api")
@PactFolder("pacts")
@Import(PactStatesProvider.class)
class PactProviderTest extends AbstractPostgresRedisIT {

    @LocalServerPort
    private int port;

    @Autowired
    private PactStatesProvider states;

    @DynamicPropertySource
    static void pactTestProperties(DynamicPropertyRegistry registry) {
        registry.add("server.compression.enabled", () -> "false");
        registry.add("app.rate-limit.enabled", () -> "false");
    }

    @BeforeEach
    void setUp(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context, HttpRequest request) {
        PactContractSupport.rewriteRequest(request, states.fixture());
        context.verifyInteraction();
    }

    @State("register email is available")
    void registerEmailIsAvailable() {
        states.registerEmailIsAvailable();
    }

    @State("customer exists with email pact-user@example.com")
    void customerExistsForLogin() {
        states.customerExistsForLogin();
    }

    @State("customer is authenticated")
    void customerIsAuthenticated() {
        states.customerIsAuthenticated();
    }

    @State("customer is logged in")
    void customerIsLoggedIn() {
        states.customerIsLoggedIn();
    }

    @State("customer has a valid refresh token")
    void customerHasValidRefreshToken() {
        states.customerHasValidRefreshToken();
    }

    @State("customer has an active account")
    void customerHasActiveAccount() {
        states.customerHasActiveAccount();
    }

    @State("customer has two active accounts")
    void customerHasTwoActiveAccounts() {
        states.customerHasTwoActiveAccounts();
    }

    @State("account has deposit history")
    void accountHasDepositHistory() {
        states.accountHasDepositHistory();
    }

    @State("admin is authenticated")
    void adminIsAuthenticated() {
        states.adminIsAuthenticated();
    }

    @State("admin can delete a customer")
    void adminCanDeleteCustomer() {
        states.adminCanDeleteCustomer();
    }

    @State("admin can freeze a customer account")
    void adminCanFreezeCustomerAccount() {
        states.adminCanFreezeCustomerAccount();
    }

    @State("admin can unfreeze a frozen account")
    void adminCanUnfreezeFrozenAccount() {
        states.adminCanUnfreezeFrozenAccount();
    }
}
