package com.company.banking.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Customer row created at registration (minimal auth-owned projection).
 */
public final class CustomerProfile {

    private final UUID id;
    private final UUID userId;
    private final String customerNumber;
    private final String fullName;
    private final String phone;
    private final String address;
    private final Instant createdAt;

    public CustomerProfile(
            UUID id,
            UUID userId,
            String customerNumber,
            String fullName,
            String phone,
            String address,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.customerNumber = Objects.requireNonNull(customerNumber, "customerNumber");
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.phone = phone;
        this.address = address;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String customerNumber() {
        return customerNumber;
    }

    public String fullName() {
        return fullName;
    }

    public String phone() {
        return phone;
    }

    public String address() {
        return address;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
