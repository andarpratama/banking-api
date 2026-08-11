package com.company.banking.audit.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SpringDataAuditLogRepository
        extends JpaRepository<AuditLogJpaEntity, UUID>, JpaSpecificationExecutor<AuditLogJpaEntity> {
}
