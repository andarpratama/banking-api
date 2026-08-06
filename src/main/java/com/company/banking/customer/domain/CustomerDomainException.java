package com.company.banking.customer.domain;

/**
 * Customer domain exceptions.
 */
public class CustomerDomainException extends RuntimeException {

    private final String errorCode;

    public CustomerDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CustomerDomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class CustomerNotFoundException extends CustomerDomainException {
        public CustomerNotFoundException(String message) {
            super("CUSTOMER_NOT_FOUND", message);
        }
    }

    public static class InvalidCustomerDataException extends CustomerDomainException {
        public InvalidCustomerDataException(String message) {
            super("INVALID_CUSTOMER_DATA", message);
        }
    }
}
