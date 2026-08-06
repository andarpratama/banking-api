package com.company.banking.customer.infrastructure.persistence;

import com.company.banking.auth.infrastructure.persistence.CustomerJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository for CustomerJpaEntity (customer feature).
 * Query only active customers (not soft-deleted).
 */
public interface SpringDataCustomerFeatureRepository extends JpaRepository<CustomerJpaEntity, UUID> {

    Optional<CustomerJpaEntity> findByCustomerNumber(String customerNumber);

    Optional<CustomerJpaEntity> findByUserId(UUID userId);

    /**
     * Find all active (non-deleted) customers with pagination.
     */
    @Query(
            value = "SELECT c FROM CustomerJpaEntity c WHERE c.deleted = false",
            countQuery = "SELECT COUNT(c) FROM CustomerJpaEntity c WHERE c.deleted = false"
    )
    Page<CustomerJpaEntity> findAllActive(Pageable pageable);

    /**
     * Count active customers.
     */
    @Query("SELECT COUNT(c) FROM CustomerJpaEntity c WHERE c.deleted = false")
    long countActive();
}
