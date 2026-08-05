package com.company.banking.auth.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenRecordTest {

    @Test
    void isUsable_withFutureExpirationAndNotRevoked_shouldReturnTrue() {
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String hash = "test-hash";
        Instant futureExpiry = Instant.now().plusSeconds(3600);

        RefreshTokenRecord token = new RefreshTokenRecord(tokenId, userId, hash, futureExpiry, false, Instant.now());

        assertThat(token.isUsable(Instant.now())).isTrue();
    }

    @Test
    void isUsable_withRevokedToken_shouldReturnFalse() {
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String hash = "test-hash";
        Instant futureExpiry = Instant.now().plusSeconds(3600);

        RefreshTokenRecord token = new RefreshTokenRecord(tokenId, userId, hash, futureExpiry, true, Instant.now());

        assertThat(token.isUsable(Instant.now())).isFalse();
    }

    @Test
    void isUsable_withExpiredToken_shouldReturnFalse() {
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String hash = "test-hash";
        Instant pastExpiry = Instant.now().minusSeconds(3600);

        RefreshTokenRecord token = new RefreshTokenRecord(tokenId, userId, hash, pastExpiry, false, Instant.now());

        assertThat(token.isUsable(Instant.now())).isFalse();
    }

    @Test
    void token_shouldBeImmutable() {
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String hash = "test-hash";
        Instant expiry = Instant.now().plusSeconds(3600);

        RefreshTokenRecord token = new RefreshTokenRecord(tokenId, userId, hash, expiry, false, Instant.now());

        assertThat(token.id()).isEqualTo(tokenId);
        assertThat(token.userId()).isEqualTo(userId);
        assertThat(token.tokenHash()).isEqualTo(hash);
        assertThat(token.expiresAt()).isEqualTo(expiry);
        assertThat(token.revoked()).isFalse();
    }

    @Test
    void isUsable_atExactExpirationTime_shouldReturnFalse() {
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String hash = "test-hash";
        Instant exactExpiry = Instant.now();

        RefreshTokenRecord token = new RefreshTokenRecord(tokenId, userId, hash, exactExpiry, false, Instant.now());

        assertThat(token.isUsable(exactExpiry)).isFalse();
    }

    @Test
    void isUsable_beforeExpirationTime_shouldReturnTrue() {
        UUID tokenId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String hash = "test-hash";
        Instant expiresAt = Instant.now().plusSeconds(3600);
        Instant checkTime = expiresAt.minusSeconds(1);

        RefreshTokenRecord token = new RefreshTokenRecord(tokenId, userId, hash, expiresAt, false, Instant.now());

        assertThat(token.isUsable(checkTime)).isTrue();
    }
}
