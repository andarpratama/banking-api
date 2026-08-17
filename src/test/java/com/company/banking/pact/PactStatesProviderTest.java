package com.company.banking.pact;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PactStatesProviderTest {

    @Test
    @DisplayName("replaces customer, account, and token placeholders from fixture")
    void replacesPlaceholders() {
        PactVerificationFixture fixture = new PactVerificationFixture();
        fixture.setCustomerId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        fixture.setAccountId("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        fixture.setDestinationAccountId("cccccccc-cccc-cccc-cccc-cccccccccccc");
        fixture.setCustomerAccessToken("customer-jwt");
        fixture.setAdminAccessToken("admin-jwt");
        fixture.setRefreshToken("refresh-jwt");

        String path = "/api/v1/customers/" + PactContractSupport.CUSTOMER_ID
                + "/accounts/" + PactContractSupport.ACCOUNT_ID;
        String body = """
                {"accountId":"%s","destinationAccountId":"%s","refreshToken":"%s"}
                """.formatted(
                PactContractSupport.ACCOUNT_ID,
                PactContractSupport.DESTINATION_ACCOUNT_ID,
                PactContractSupport.REFRESH_TOKEN
        );
        String auth = "Bearer " + PactContractSupport.CUSTOMER_TOKEN;
        String adminAuth = "Bearer " + PactContractSupport.ADMIN_TOKEN;

        assertThat(PactContractSupport.replacePlaceholders(path, fixture))
                .isEqualTo("/api/v1/customers/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
                        + "/accounts/bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        assertThat(PactContractSupport.replacePlaceholders(body, fixture))
                .contains("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
                .contains("cccccccc-cccc-cccc-cccc-cccccccccccc")
                .contains("refresh-jwt");
        assertThat(PactContractSupport.replacePlaceholders(auth, fixture))
                .isEqualTo("Bearer customer-jwt");
        assertThat(PactContractSupport.replacePlaceholders(adminAuth, fixture))
                .isEqualTo("Bearer admin-jwt");
    }

    @Test
    @DisplayName("leaves text unchanged when fixture values are missing")
    void skipsNullFixtureValues() {
        PactVerificationFixture fixture = new PactVerificationFixture();
        String original = "/api/v1/customers/" + PactContractSupport.CUSTOMER_ID;

        assertThat(PactContractSupport.replacePlaceholders(original, fixture)).isEqualTo(original);
        assertThat(PactContractSupport.replacePlaceholders(original, null)).isEqualTo(original);
        assertThat(PactContractSupport.replacePlaceholders(null, fixture)).isNull();
    }

    @Test
    @DisplayName("contract emails contain no secrets")
    void contractEmailsAreFixtures() {
        assertThat(PactContractSupport.PASSWORD).doesNotContain("prod");
        assertThat(PactContractSupport.CUSTOMER_EMAIL).endsWith("@example.com");
        assertThat(PactContractSupport.ADMIN_EMAIL).endsWith("@example.com");
        assertThat(PactContractSupport.REGISTER_EMAIL).endsWith("@example.com");
    }
}
