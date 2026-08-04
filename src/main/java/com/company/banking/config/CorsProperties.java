package com.company.banking.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * CORS whitelist settings. Origins come from env / profile YAML — never use {@code *}
 * when {@link #allowCredentials} is true (browser credentialed requests).
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Exact allowed Origin values (e.g. {@code http://localhost:5173}). Empty = no CORS.
     */
    private List<String> allowedOrigins = new ArrayList<>();

    private List<String> allowedMethods =
            new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));

    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

    private boolean allowCredentials = true;

    /** Preflight cache duration in seconds. */
    private long maxAge = 3600L;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = normalizeOriginList(allowedOrigins);
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods != null ? allowedMethods : new ArrayList<>();
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders != null ? allowedHeaders : new ArrayList<>();
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * Accepts YAML lists and comma-separated env placeholders (often one list element).
     */
    private static List<String> normalizeOriginList(List<String> origins) {
        List<String> normalized = new ArrayList<>();
        if (origins == null) {
            return normalized;
        }
        for (String origin : origins) {
            if (!StringUtils.hasText(origin)) {
                continue;
            }
            for (String part : origin.split(",")) {
                if (StringUtils.hasText(part)) {
                    normalized.add(part.trim());
                }
            }
        }
        return normalized;
    }
}
