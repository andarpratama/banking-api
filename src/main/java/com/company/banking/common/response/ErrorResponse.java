package com.company.banking.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Standard API error body (OpenAPI §8 Error Response Format).
 */
@Schema(description = "Standard API error envelope")
public record ErrorResponse(
        @Schema(example = "2026-08-04T12:00:00Z")
        Instant timestamp,
        @Schema(example = "401")
        int status,
        @Schema(example = "INVALID_CREDENTIALS")
        String code,
        @Schema(example = "Invalid email or password")
        String message,
        @Schema(example = "/api/v1/auth/login")
        String path
) {
}
