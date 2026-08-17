package com.company.banking.common.presentation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Liveness payload for {@code GET /api/v1/health}.
 */
@Schema(description = "Liveness / legacy health payload")
public record HealthResponse(
        @Schema(example = "UP", description = "Process liveness status")
        String status
) {

    public static final String UP = "UP";

    public static HealthResponse up() {
        return new HealthResponse(UP);
    }
}
