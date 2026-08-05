package com.company.banking.auth.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataCustomerRepository extends JpaRepository<CustomerJpaEntity, UUID> {

    @Query(
            value = "SELECT COALESCE(MAX(CAST(SUBSTRING(customer_number FROM 6) AS INTEGER)), 0) FROM customers",
            nativeQuery = true
    )
    int findMaxCustomerSequence();
}
