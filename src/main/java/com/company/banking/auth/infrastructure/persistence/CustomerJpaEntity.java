package com.company.banking.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class CustomerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "customer_number", nullable = false, unique = true, length = 50)
    private String customerNumber;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerJpaEntity() {
    }

    public CustomerJpaEntity(
            UUID userId,
            String customerNumber,
            String fullName,
            String phone,
            String address,
            Instant now
    ) {
        this.userId = userId;
        this.customerNumber = customerNumber;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.status = "ACTIVE";
        this.deleted = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void applyProfile(String fullName, String phone, String address, Instant updatedAt) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.updatedAt = updatedAt;
    }

    public void applyLifecycle(String status, boolean deleted, Instant updatedAt) {
        this.status = status;
        this.deleted = deleted;
        this.updatedAt = updatedAt;
    }
}
