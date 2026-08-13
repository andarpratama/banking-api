package com.company.banking.pact;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

/**
 * Placeholder tokens used in checked-in Pact files. Replaced at verification time
 * with IDs and JWTs produced by {@link PactStatesProvider}.
 */
public final class PactContractSupport {

    public static final String CUSTOMER_ID = "11111111-1111-1111-1111-111111111111";
    public static final String ACCOUNT_ID = "22222222-2222-2222-2222-222222222222";
    public static final String DESTINATION_ACCOUNT_ID = "33333333-3333-3333-3333-333333333333";
    public static final String CUSTOMER_TOKEN = "pact-customer-token";
    public static final String ADMIN_TOKEN = "pact-admin-token";
    public static final String REFRESH_TOKEN = "pact-refresh-token";

    public static final String REGISTER_EMAIL = "pact-new@example.com";
    public static final String CUSTOMER_EMAIL = "pact-user@example.com";
    public static final String ADMIN_EMAIL = "pact-admin@example.com";
    public static final String DELETE_EMAIL = "pact-delete@example.com";
    public static final String PASSWORD = "SecurePass123!";
    public static final String CUSTOMER_NAME = "Pact User";
    public static final String ADMIN_NAME = "Pact Admin";
    public static final String PHONE = "+15550001111";
    public static final String ADDRESS = "1 Test Street";

    private PactContractSupport() {
    }

    public static String replacePlaceholders(String value, PactVerificationFixture fixture) {
        if (value == null || fixture == null) {
            return value;
        }
        String result = value;
        result = replace(result, CUSTOMER_ID, fixture.customerId());
        result = replace(result, ACCOUNT_ID, fixture.accountId());
        result = replace(result, DESTINATION_ACCOUNT_ID, fixture.destinationAccountId());
        result = replace(result, CUSTOMER_TOKEN, fixture.customerAccessToken());
        result = replace(result, ADMIN_TOKEN, fixture.adminAccessToken());
        result = replace(result, REFRESH_TOKEN, fixture.refreshToken());
        return result;
    }

    public static void rewriteRequest(HttpRequest request, PactVerificationFixture fixture) {
        if (request == null || fixture == null) {
            return;
        }
        rewriteAuthorization(request, fixture);
        rewritePath(request, fixture);
        if (request instanceof ClassicHttpRequest classic) {
            rewriteBody(classic, fixture);
        }
    }

    private static void rewriteAuthorization(HttpRequest request, PactVerificationFixture fixture) {
        Header header = request.getFirstHeader("Authorization");
        if (header == null) {
            return;
        }
        String rewritten = replacePlaceholders(header.getValue(), fixture);
        if (!header.getValue().equals(rewritten)) {
            request.setHeader("Authorization", rewritten);
        }
    }

    private static void rewritePath(HttpRequest request, PactVerificationFixture fixture) {
        try {
            URI uri = request.getUri();
            String raw = uri.toString();
            String rewritten = replacePlaceholders(raw, fixture);
            if (!raw.equals(rewritten)) {
                request.setUri(URI.create(rewritten));
            }
        } catch (URISyntaxException ex) {
            String path = request.getPath();
            String rewritten = replacePlaceholders(path, fixture);
            if (!path.equals(rewritten)) {
                request.setPath(rewritten);
            }
        }
    }

    private static void rewriteBody(ClassicHttpRequest request, PactVerificationFixture fixture) {
        HttpEntity entity = request.getEntity();
        if (entity == null) {
            return;
        }
        try {
            String body = EntityUtils.toString(entity);
            String rewritten = replacePlaceholders(body, fixture);
            if (!body.equals(rewritten)) {
                request.setEntity(new StringEntity(rewritten, ContentType.APPLICATION_JSON));
            }
        } catch (java.io.IOException | ParseException ex) {
            throw new IllegalStateException("Failed to rewrite Pact request body", ex);
        }
    }

    private static String replace(String value, String placeholder, String replacement) {
        if (replacement == null || replacement.isBlank()) {
            return value;
        }
        return value.replace(placeholder, replacement);
    }
}
