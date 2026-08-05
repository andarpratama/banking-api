package com.company.banking.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshTokenRecord save(UUID userId, String tokenHash, Instant expiresAt);

    Optional<RefreshTokenRecord> findByTokenHash(String tokenHash);

    void revoke(UUID id);

    void revokeAllForUser(UUID userId);
}
