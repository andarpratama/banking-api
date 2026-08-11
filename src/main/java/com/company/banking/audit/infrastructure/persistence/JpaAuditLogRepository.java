package com.company.banking.audit.infrastructure.persistence;

import com.company.banking.audit.domain.AuditLog;
import com.company.banking.audit.domain.AuditLogRepository;
import com.company.banking.audit.domain.AuditStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAuditLogRepository implements AuditLogRepository {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "createdAt", "createdAt",
            "actor", "actor",
            "endpoint", "endpoint",
            "action", "action",
            "status", "status",
            "statusCode", "statusCode"
    );

    private final SpringDataAuditLogRepository springDataRepository;

    public JpaAuditLogRepository(SpringDataAuditLogRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity(
                auditLog.id(),
                auditLog.actor(),
                auditLog.endpoint(),
                auditLog.method(),
                auditLog.action(),
                auditLog.statusCode(),
                auditLog.status().name(),
                auditLog.ipAddress(),
                auditLog.payloadHash(),
                auditLog.createdAt()
        );
        AuditLogJpaEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<AuditLog> findFiltered(
            String actor,
            String endpoint,
            AuditStatus status,
            Instant fromDate,
            Instant toDate,
            int page,
            int size,
            String sortProperty,
            boolean ascending
    ) {
        String column = SORT_COLUMNS.getOrDefault(sortProperty, "createdAt");
        Sort sort = ascending ? Sort.by(column).ascending() : Sort.by(column).descending();
        Specification<AuditLogJpaEntity> spec = filterSpec(actor, endpoint, status, fromDate, toDate);
        return springDataRepository.findAll(spec, PageRequest.of(page, size, sort))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countFiltered(
            String actor,
            String endpoint,
            AuditStatus status,
            Instant fromDate,
            Instant toDate
    ) {
        return springDataRepository.count(filterSpec(actor, endpoint, status, fromDate, toDate));
    }

    private static Specification<AuditLogJpaEntity> filterSpec(
            String actor,
            String endpoint,
            AuditStatus status,
            Instant fromDate,
            Instant toDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actor != null) {
                predicates.add(cb.equal(cb.lower(root.get("actor")), actor.toLowerCase()));
            }
            if (endpoint != null) {
                predicates.add(cb.equal(root.get("endpoint"), endpoint));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status.name()));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private AuditLog toDomain(AuditLogJpaEntity entity) {
        return new AuditLog(
                entity.getId(),
                entity.getActor(),
                entity.getEndpoint(),
                entity.getMethod(),
                entity.getAction(),
                entity.getStatusCode(),
                AuditStatus.valueOf(entity.getStatus()),
                entity.getIpAddress(),
                entity.getPayloadHash(),
                entity.getCreatedAt()
        );
    }
}
