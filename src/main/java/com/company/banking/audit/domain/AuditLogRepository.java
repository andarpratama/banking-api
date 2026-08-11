package com.company.banking.audit.domain;

import java.time.Instant;
import java.util.List;

/**
 * Persistence port for audit logs (append + filtered list).
 */
public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findFiltered(
            String actor,
            String endpoint,
            AuditStatus status,
            Instant fromDate,
            Instant toDate,
            int page,
            int size,
            String sortProperty,
            boolean ascending
    );

    long countFiltered(
            String actor,
            String endpoint,
            AuditStatus status,
            Instant fromDate,
            Instant toDate
    );
}
