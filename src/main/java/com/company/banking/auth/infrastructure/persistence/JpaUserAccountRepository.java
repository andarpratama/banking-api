package com.company.banking.auth.infrastructure.persistence;

import com.company.banking.auth.domain.UserAccount;
import com.company.banking.auth.domain.UserAccountRepository;
import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaUserAccountRepository implements UserAccountRepository {

    private final SpringDataUserRepository users;
    private final SpringDataRoleRepository roles;

    public JpaUserAccountRepository(SpringDataUserRepository users, SpringDataRoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.existsByEmailIgnoreCase(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        return users.findByEmailIgnoreCase(email).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(UUID id) {
        return users.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public UserAccount save(String email, String passwordHash, String roleName) {
        RoleJpaEntity role = roles.findByName(roleName)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "Required role is not configured: " + roleName
                ));
        Instant now = Instant.now();
        UserJpaEntity entity = new UserJpaEntity(email, passwordHash, now);
        entity.addRole(role);
        return toDomain(users.save(entity));
    }

    private UserAccount toDomain(UserJpaEntity entity) {
        Set<String> roleNames = entity.getRoles().stream()
                .map(RoleJpaEntity::getName)
                .collect(Collectors.toUnmodifiableSet());
        return new UserAccount(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                roleNames
        );
    }
}
