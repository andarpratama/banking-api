package com.company.banking.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted refresh-token metadata (hash only).
 */
public final class RefreshTokenRecord {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final boolean revoked;
    private final Instant createdAt;

    public RefreshTokenRecord(
            UUID id,
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            boolean revoked,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.revoked = revoked;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean revoked() {
        return revoked;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean isUsable(Instant now) {
        return !revoked && expiresAt.isAfter(now);
    }

    public RefreshTokenRecord revokedCopy() {
        return new RefreshTokenRecord(id, userId, tokenHash, expiresAt, true, createdAt);
    }
}
