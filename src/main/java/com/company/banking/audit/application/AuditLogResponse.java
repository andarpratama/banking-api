package com.company.banking.audit.application;

/**
 * Response DTO aligned with OpenAPI §7.1 audit log item.
 */
public record AuditLogResponse(
        String id,
        String actor,
        String endpoint,
        String method,
        String action,
        int statusCode,
        String status,
        String ipAddress,
        String payloadHash,
        String createdAt
) {
}
