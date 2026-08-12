-- Align audit_logs with OpenAPI / enhanced DDD (actor, endpoint, method, status_code, payload_hash).
-- No production writers existed against the V1 entity-centric shape; recreate safely.

DROP TABLE IF EXISTS audit_logs;

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor VARCHAR(255) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    action VARCHAR(100) NOT NULL,
    status_code INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS'
        CHECK (status IN ('SUCCESS', 'FAILURE')),
    ip_address VARCHAR(45),
    payload_hash VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_endpoint ON audit_logs(endpoint);
CREATE INDEX idx_audit_logs_status ON audit_logs(status);
