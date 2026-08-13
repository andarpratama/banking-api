package com.company.banking.pact;

/**
 * IDs and tokens substituted into Pact requests after provider state setup.
 */
final class PactVerificationFixture {

    private String customerId;
    private String accountId;
    private String destinationAccountId;
    private String customerAccessToken;
    private String adminAccessToken;
    private String refreshToken;

    String customerId() {
        return customerId;
    }

    void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    String accountId() {
        return accountId;
    }

    void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    String destinationAccountId() {
        return destinationAccountId;
    }

    void setDestinationAccountId(String destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    String customerAccessToken() {
        return customerAccessToken;
    }

    void setCustomerAccessToken(String customerAccessToken) {
        this.customerAccessToken = customerAccessToken;
    }

    String adminAccessToken() {
        return adminAccessToken;
    }

    void setAdminAccessToken(String adminAccessToken) {
        this.adminAccessToken = adminAccessToken;
    }

    String refreshToken() {
        return refreshToken;
    }

    void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
