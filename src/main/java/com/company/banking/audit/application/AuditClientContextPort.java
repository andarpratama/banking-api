package com.company.banking.audit.application;

/**
 * Optional client metadata for audit enrichment (IP). Implemented in infrastructure.
 */
public interface AuditClientContextPort {

    /**
     * @return client IP if an HTTP request is in scope; otherwise {@code null}
     */
    String clientIp();
}
