package com.company.banking.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.banking.audit.domain.AuditLog;
import com.company.banking.audit.domain.AuditStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditMapperTest {

    @Test
    void toResponseMapsAllFields() {
        UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        Instant createdAt = Instant.parse("2026-08-04T12:10:00Z");
        AuditLog log = new AuditLog(
                id,
                "john@example.com",
                "/transactions/transfer",
                "POST",
                "TRANSFER_MONEY",
                200,
                AuditStatus.SUCCESS,
                "192.168.1.100",
                "abc123hash",
                createdAt
        );

        AuditLogResponse response = AuditMapper.toResponse(log);

        assertThat(response.id()).isEqualTo(id.toString());
        assertThat(response.actor()).isEqualTo("john@example.com");
        assertThat(response.endpoint()).isEqualTo("/transactions/transfer");
        assertThat(response.method()).isEqualTo("POST");
        assertThat(response.action()).isEqualTo("TRANSFER_MONEY");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.ipAddress()).isEqualTo("192.168.1.100");
        assertThat(response.payloadHash()).isEqualTo("abc123hash");
        assertThat(response.createdAt()).isEqualTo("2026-08-04T12:10:00Z");
    }
}
