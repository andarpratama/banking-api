package com.company.banking.auth.infrastructure.persistence;

import com.company.banking.auth.domain.RefreshTokenRecord;
import com.company.banking.auth.domain.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository tokens;

    public JpaRefreshTokenRepository(SpringDataRefreshTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    @Transactional
    public RefreshTokenRecord save(UUID userId, String tokenHash, Instant expiresAt) {
        Instant now = Instant.now();
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity(userId, tokenHash, expiresAt, now);
        return toDomain(tokens.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshTokenRecord> findByTokenHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional
    public void revoke(UUID id) {
        tokens.findById(id).ifPresent(entity -> {
            entity.revoke();
            tokens.save(entity);
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(UUID userId) {
        tokens.revokeAllActiveForUser(userId);
    }

    private RefreshTokenRecord toDomain(RefreshTokenJpaEntity entity) {
        return new RefreshTokenRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.isRevoked(),
                entity.getCreatedAt()
        );
    }
}
