package com.company.banking.common.exception;

import com.company.banking.common.constants.ErrorCode;

/**
 * Domain/application failure mapped to a stable {@link ErrorCode} and HTTP status.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
