package com.company.banking.audit.application;

import com.company.banking.audit.domain.AuditStatus;
import java.util.Objects;

/**
 * Command to append one audit log entry.
 */
public record RecordAuditCommand(
        String actor,
        String endpoint,
        String method,
        String action,
        int statusCode,
        AuditStatus status,
        String ipAddress,
        String payloadHash
) {

    public RecordAuditCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(status, "status");
    }

    public static RecordAuditCommand of(
            String actor,
            String endpoint,
            String method,
            String action,
            int statusCode,
            String ipAddress,
            String payloadHash
    ) {
        return new RecordAuditCommand(
                actor,
                endpoint,
                method,
                action,
                statusCode,
                AuditStatus.fromHttpStatus(statusCode),
                ipAddress,
                payloadHash
        );
    }
}
