package com.company.banking.audit.application;

import com.company.banking.audit.domain.AuditLog;
import java.time.format.DateTimeFormatter;

/**
 * Maps domain audit logs to API responses.
 */
public final class AuditMapper {

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    private AuditMapper() {
    }

    public static AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.id().toString(),
                log.actor(),
                log.endpoint(),
                log.method(),
                log.action(),
                log.statusCode(),
                log.status().name(),
                log.ipAddress(),
                log.payloadHash(),
                ISO_INSTANT.format(log.createdAt())
        );
    }
}
