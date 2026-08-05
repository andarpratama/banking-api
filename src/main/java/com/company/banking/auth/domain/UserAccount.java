package com.company.banking.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Authenticated account (users table) — framework-free.
 */
public final class UserAccount {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Set<String> roles;

    public UserAccount(
            UUID id,
            String email,
            String passwordHash,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt,
            Set<String> roles
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.enabled = enabled;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public boolean enabled() {
        return enabled;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Set<String> roles() {
        return roles;
    }
}
