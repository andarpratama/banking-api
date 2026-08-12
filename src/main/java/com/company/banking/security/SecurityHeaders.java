package com.company.banking.security;

/**
 * v1 security header values for a JSON REST API (see Security & Performance guidelines).
 */
public final class SecurityHeaders {

    public static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'; form-action 'self'";

    public static final String REFERRER_POLICY = "no-referrer";

    public static final String PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=()";

    /** HSTS: 1 year, include subdomains (browsers apply only over HTTPS). */
    public static final long HSTS_MAX_AGE_SECONDS = 31_536_000L;

    /** Matches Spring Security's Strict-Transport-Security header writer formatting. */
    public static final String HSTS_VALUE = "max-age=" + HSTS_MAX_AGE_SECONDS + " ; includeSubDomains";

    public static final String X_CONTENT_TYPE_OPTIONS = "nosniff";

    public static final String X_FRAME_OPTIONS = "DENY";

    private SecurityHeaders() {
    }
}
