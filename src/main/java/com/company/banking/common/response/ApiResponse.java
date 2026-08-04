package com.company.banking.common.response;

import java.time.Instant;

/**
 * Generic success envelope for endpoints that wrap a payload.
 */
public record ApiResponse<T>(
        Instant timestamp,
        boolean success,
        T data,
        String message
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(Instant.now(), true, data, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(Instant.now(), true, data, message);
    }
}
