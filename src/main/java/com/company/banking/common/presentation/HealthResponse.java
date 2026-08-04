package com.company.banking.common.presentation;

/**
 * Liveness payload for {@code GET /api/v1/health}.
 */
public record HealthResponse(String status) {

    public static final String UP = "UP";

    public static HealthResponse up() {
        return new HealthResponse(UP);
    }
}
