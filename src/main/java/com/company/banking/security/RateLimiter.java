package com.company.banking.security;

/**
 * Fixed-window counter used by {@link RateLimitFilter}.
 */
public interface RateLimiter {

    /**
     * Increments the counter for {@code key} and returns whether the request is allowed.
     *
     * @param key           bucket identity (e.g. {@code global:ip:1.2.3.4})
     * @param limit         max requests in the window
     * @param windowSeconds window length in seconds
     */
    RateLimitResult tryConsume(String key, int limit, int windowSeconds);
}
