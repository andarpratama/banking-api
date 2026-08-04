package com.company.banking.common.response;

import java.time.Instant;

/**
 * Standard API error body (OpenAPI §8 Error Response Format).
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
}
