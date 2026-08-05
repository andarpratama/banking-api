package com.company.banking.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshTokenJpaEntity r set r.revoked = true where r.userId = :userId and r.revoked = false")
    int revokeAllActiveForUser(@Param("userId") UUID userId);
}
