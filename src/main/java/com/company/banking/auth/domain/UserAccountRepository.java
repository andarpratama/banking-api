package com.company.banking.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository {

    boolean existsByEmail(String email);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(UUID id);

    UserAccount save(String email, String passwordHash, String roleName);
}
