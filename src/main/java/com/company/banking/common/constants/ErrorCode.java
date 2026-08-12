package com.company.banking.common.constants;

/**
 * Stable API error codes aligned with the OpenAPI error catalog,
 * plus baseline codes used by the global exception handler.
 */
public enum ErrorCode {

    VALIDATION_ERROR(400),
    NOT_FOUND(404),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    INTERNAL_ERROR(500),

    INVALID_CREDENTIALS(401),
    INVALID_TOKEN(401),
    CUSTOMER_NOT_FOUND(404),
    ACCOUNT_NOT_FOUND(404),
    ACCOUNT_FROZEN(409),
    ACCOUNT_CLOSED(409),
    INSUFFICIENT_BALANCE(409),
    INVALID_AMOUNT(400),
    DUPLICATE_EMAIL(400),
    SAME_ACCOUNT_TRANSFER(409),
    OPTIMISTIC_LOCK_EXCEPTION(409),
    RATE_LIMIT_EXCEEDED(429);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
