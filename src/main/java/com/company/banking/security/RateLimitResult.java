package com.company.banking.security;

/**
 * Outcome of a single rate-limit check (fixed window).
 */
public record RateLimitResult(
        boolean allowed,
        int limit,
        int remaining,
        long resetEpochSeconds
) {
}
