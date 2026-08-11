package com.company.banking.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 255)
    private String actor;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "payload_hash", length = 255)
    private String payloadHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLogJpaEntity() {
    }

    public AuditLogJpaEntity(
            UUID id,
            String actor,
            String endpoint,
            String method,
            String action,
            Integer statusCode,
            String status,
            String ipAddress,
            String payloadHash,
            Instant createdAt
    ) {
        this.id = id;
        this.actor = actor;
        this.endpoint = endpoint;
        this.method = method;
        this.action = action;
        this.statusCode = statusCode;
        this.status = status;
        this.ipAddress = ipAddress;
        this.payloadHash = payloadHash;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getMethod() {
        return method;
    }

    public String getAction() {
        return action;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getStatus() {
        return status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
