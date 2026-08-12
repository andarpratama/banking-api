package com.company.banking.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit log entry — append-only compliance trail.
 */
public final class AuditLog {

    private final UUID id;
    private final String actor;
    private final String endpoint;
    private final String method;
    private final String action;
    private final int statusCode;
    private final AuditStatus status;
    private final String ipAddress;
    private final String payloadHash;
    private final Instant createdAt;

    public AuditLog(
            UUID id,
            String actor,
            String endpoint,
            String method,
            String action,
            int statusCode,
            AuditStatus status,
            String ipAddress,
            String payloadHash,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.actor = Objects.requireNonNull(actor, "actor");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.method = Objects.requireNonNull(method, "method");
        this.action = Objects.requireNonNull(action, "action");
        this.statusCode = statusCode;
        this.status = Objects.requireNonNull(status, "status");
        this.ipAddress = ipAddress;
        this.payloadHash = payloadHash;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static AuditLog create(
            String actor,
            String endpoint,
            String method,
            String action,
            int statusCode,
            AuditStatus status,
            String ipAddress,
            String payloadHash,
            Instant createdAt
    ) {
        return new AuditLog(
                UUID.randomUUID(),
                actor,
                endpoint,
                method,
                action,
                statusCode,
                status,
                ipAddress,
                payloadHash,
                createdAt
        );
    }

    public UUID id() {
        return id;
    }

    public String actor() {
        return actor;
    }

    public String endpoint() {
        return endpoint;
    }

    public String method() {
        return method;
    }

    public String action() {
        return action;
    }

    public int statusCode() {
        return statusCode;
    }

    public AuditStatus status() {
        return status;
    }

    public String ipAddress() {
        return ipAddress;
    }

    public String payloadHash() {
        return payloadHash;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
