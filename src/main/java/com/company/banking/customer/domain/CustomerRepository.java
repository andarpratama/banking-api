package com.company.banking.customer.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Customer repository interface — defined in domain, implemented in infrastructure.
 */
public interface CustomerRepository {

    Optional<Customer> findById(UUID id);

    Optional<Customer> findByCustomerNumber(String customerNumber);

    Optional<Customer> findByUserId(UUID userId);

    Customer save(Customer customer);

    /**
     * Find all active customers with pagination/sorting support.
     * Skip soft-deleted customers.
     */
    List<Customer> findAllActive(int page, int size, String sortBy, String sortDirection);

    /**
     * Count total active customers (for pagination).
     */
    long countActive();
}
