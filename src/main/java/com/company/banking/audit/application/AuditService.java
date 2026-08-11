package com.company.banking.audit.application;

import com.company.banking.audit.domain.AuditLog;
import com.company.banking.audit.domain.AuditLogRepository;
import com.company.banking.audit.domain.AuditStatus;
import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.common.pagination.PageQuery;
import com.company.banking.common.pagination.PageResponse;
import com.company.banking.common.pagination.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records sensitive actions and lists audit logs for ADMIN.
 */
@Service
public class AuditService {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "createdAt",
            "actor",
            "endpoint",
            "action",
            "status",
            "statusCode"
    );

    private final AuditLogRepository auditLogRepository;
    private final AuditClientContextPort clientContext;

    public AuditService(AuditLogRepository auditLogRepository, AuditClientContextPort clientContext) {
        this.auditLogRepository = auditLogRepository;
        this.clientContext = clientContext;
    }

    /**
     * Persist an audit entry in a new transaction so failures (e.g. bad login) still leave a trail.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(RecordAuditCommand command) {
        String ip = command.ipAddress() != null ? command.ipAddress() : clientContext.clientIp();
        AuditLog entry = AuditLog.create(
                command.actor(),
                command.endpoint(),
                command.method(),
                command.action(),
                command.statusCode(),
                command.status(),
                ip,
                command.payloadHash(),
                Instant.now()
        );
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listAuditLogs(
            Integer page,
            Integer size,
            String sort,
            String actor,
            String endpoint,
            String status,
            Instant fromDate,
            Instant toDate
    ) {
        PageQuery query = PageQuery.of(page, size, sort);
        if (!ALLOWED_SORT.contains(query.sort().property())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported sort property");
        }

        AuditStatus statusFilter = parseStatus(status);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "fromDate must be <= toDate");
        }

        boolean ascending = query.sort().direction() == SortDirection.ASC;
        String sortProperty = query.sort().property();

        List<AuditLog> logs = auditLogRepository.findFiltered(
                blankToNull(actor),
                blankToNull(endpoint),
                statusFilter,
                fromDate,
                toDate,
                query.page(),
                query.size(),
                sortProperty,
                ascending
        );
        long total = auditLogRepository.countFiltered(
                blankToNull(actor),
                blankToNull(endpoint),
                statusFilter,
                fromDate,
                toDate
        );

        List<AuditLogResponse> content = logs.stream().map(AuditMapper::toResponse).toList();
        return PageResponse.of(content, total, query);
    }

    private static AuditStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return AuditStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "status must be SUCCESS or FAILURE"
            );
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
