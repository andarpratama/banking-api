package com.company.banking.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.banking.audit.domain.AuditActions;
import com.company.banking.audit.domain.AuditLog;
import com.company.banking.audit.domain.AuditLogRepository;
import com.company.banking.audit.domain.AuditStatus;
import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.common.pagination.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditServiceTest {

    private AuditLogRepository auditLogRepository;
    private AuditClientContextPort clientContext;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        clientContext = mock(AuditClientContextPort.class);
        auditService = new AuditService(auditLogRepository, clientContext);
    }

    @Test
    void recordPersistsEntryAndEnrichesIpFromContext() {
        when(clientContext.clientIp()).thenReturn("10.0.0.5");
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.record(RecordAuditCommand.of(
                "admin@example.com",
                "/accounts",
                "POST",
                AuditActions.CREATE_ACCOUNT,
                201,
                null,
                "payloadhash"
        ));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.actor()).isEqualTo("admin@example.com");
        assertThat(saved.action()).isEqualTo(AuditActions.CREATE_ACCOUNT);
        assertThat(saved.status()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(saved.statusCode()).isEqualTo(201);
        assertThat(saved.ipAddress()).isEqualTo("10.0.0.5");
        assertThat(saved.payloadHash()).isEqualTo("payloadhash");
    }

    @Test
    void recordUsesExplicitIpOverContext() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.record(RecordAuditCommand.of(
                "user@example.com",
                "/auth/login",
                "POST",
                AuditActions.LOGIN,
                401,
                "203.0.113.9",
                null
        ));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().ipAddress()).isEqualTo("203.0.113.9");
        assertThat(captor.getValue().status()).isEqualTo(AuditStatus.FAILURE);
    }

    @Test
    void listAuditLogsMapsRepositoryResults() {
        AuditLog log = AuditLog.create(
                "john@example.com",
                "/transactions/transfer",
                "POST",
                AuditActions.TRANSFER_MONEY,
                200,
                AuditStatus.SUCCESS,
                "127.0.0.1",
                "hash",
                Instant.parse("2026-08-04T12:10:00Z")
        );
        when(auditLogRepository.findFiltered(
                eq("john@example.com"),
                eq("/transactions/transfer"),
                eq(AuditStatus.SUCCESS),
                isNull(),
                isNull(),
                eq(0),
                eq(20),
                eq("createdAt"),
                eq(false)
        )).thenReturn(List.of(log));
        when(auditLogRepository.countFiltered(
                eq("john@example.com"),
                eq("/transactions/transfer"),
                eq(AuditStatus.SUCCESS),
                isNull(),
                isNull()
        )).thenReturn(1L);

        PageResponse<AuditLogResponse> page = auditService.listAuditLogs(
                0,
                20,
                "createdAt,desc",
                "john@example.com",
                "/transactions/transfer",
                "SUCCESS",
                null,
                null
        );

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().action()).isEqualTo(AuditActions.TRANSFER_MONEY);
        assertThat(page.content().getFirst().id()).isEqualTo(log.id().toString());
    }

    @Test
    void listAuditLogsRejectsInvalidStatus() {
        assertThatThrownBy(() -> auditService.listAuditLogs(
                0, 20, null, null, null, "NOPE", null, null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void payloadHasherProducesStableSha256() {
        String hash = AuditPayloadHasher.sha256("accountId=" + UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        ));
        assertThat(hash).hasSize(64);
        assertThat(AuditPayloadHasher.sha256("accountId=11111111-1111-1111-1111-111111111111"))
                .isEqualTo(hash);
        assertThat(AuditPayloadHasher.sha256("")).isNull();
    }
}
