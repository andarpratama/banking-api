package com.company.banking.common.exception;

import com.company.banking.common.constants.ErrorCode;

/**
 * Resource missing — defaults to {@link ErrorCode#NOT_FOUND}.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
