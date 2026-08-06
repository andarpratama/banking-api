package com.company.banking.customer.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Customer domain entity — represents a customer record with profile and status.
 * Immutable value object pattern.
 */
public final class Customer {

    private final UUID id;
    private final UUID userId;
    private final String customerNumber;
    private final String fullName;
    private final String phone;
    private final String address;
    private final CustomerStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Customer(
            UUID id,
            UUID userId,
            String customerNumber,
            String fullName,
            String phone,
            String address,
            CustomerStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.customerNumber = Objects.requireNonNull(customerNumber, "customerNumber");
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.phone = phone;
        this.address = address;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
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

    public CustomerStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return status == CustomerStatus.SOFT_DELETED;
    }

    /**
     * Update customer profile (returns new Customer instance).
     */
    public Customer updateProfile(String fullName, String phone, String address, Instant now) {
        return new Customer(
                this.id,
                this.userId,
                this.customerNumber,
                Objects.requireNonNull(fullName, "fullName"),
                phone,
                address,
                this.status,
                this.createdAt,
                now
        );
    }

    /**
     * Soft delete customer (returns new Customer instance).
     */
    public Customer delete(Instant now) {
        return new Customer(
                this.id,
                this.userId,
                this.customerNumber,
                this.fullName,
                this.phone,
                this.address,
                CustomerStatus.SOFT_DELETED,
                this.createdAt,
                now
        );
    }
}
