package com.company.banking.audit.domain;

/**
 * Outcome of an audited action (OpenAPI filter/response {@code status}).
 */
public enum AuditStatus {
    SUCCESS,
    FAILURE;

    public static AuditStatus fromHttpStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 400 ? SUCCESS : FAILURE;
    }
}
