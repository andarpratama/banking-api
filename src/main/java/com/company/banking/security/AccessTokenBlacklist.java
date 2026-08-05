package com.company.banking.security;

import java.time.Instant;

/**
 * Revoked access tokens (logout) until natural JWT expiry.
 */
public interface AccessTokenBlacklist {

    void blacklist(String rawToken, Instant expiresAt);

    boolean isBlacklisted(String rawToken);
}
