package com.company.banking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Fixed-window rate limits. Keys are per authenticated user when present, otherwise client IP.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /**
     * Backend: {@code redis} (default) or {@code memory} (dev/test without Redis).
     */
    private String backend = "redis";

    private boolean enabled = true;

    /** Global API limit (OpenAPI §10): requests per window. */
    private int globalLimit = 100;

    private int globalWindowSeconds = 60;

    /** Stricter limit for {@code /api/v1/auth/**} (login/register abuse). */
    private int authLimit = 20;

    private int authWindowSeconds = 60;

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getGlobalLimit() {
        return globalLimit;
    }

    public void setGlobalLimit(int globalLimit) {
        this.globalLimit = globalLimit;
    }

    public int getGlobalWindowSeconds() {
        return globalWindowSeconds;
    }

    public void setGlobalWindowSeconds(int globalWindowSeconds) {
        this.globalWindowSeconds = globalWindowSeconds;
    }

    public int getAuthLimit() {
        return authLimit;
    }

    public void setAuthLimit(int authLimit) {
        this.authLimit = authLimit;
    }

    public int getAuthWindowSeconds() {
        return authWindowSeconds;
    }

    public void setAuthWindowSeconds(int authWindowSeconds) {
        this.authWindowSeconds = authWindowSeconds;
    }
}
