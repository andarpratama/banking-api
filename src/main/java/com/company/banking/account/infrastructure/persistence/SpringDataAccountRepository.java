package com.company.banking.account.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataAccountRepository extends JpaRepository<AccountJpaEntity, UUID> {

    Optional<AccountJpaEntity> findByAccountNumber(String accountNumber);

    List<AccountJpaEntity> findByCustomerIdOrderByCreatedAtAsc(UUID customerId);

    long countByCustomerId(UUID customerId);

    @Query(
            value = "SELECT COALESCE(MAX(CAST(SUBSTRING(account_number FROM 5) AS INTEGER)), 0) FROM accounts",
            nativeQuery = true
    )
    int findMaxAccountSequence();
}
