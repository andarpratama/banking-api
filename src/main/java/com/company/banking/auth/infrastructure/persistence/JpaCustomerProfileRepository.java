package com.company.banking.auth.infrastructure.persistence;

import com.company.banking.auth.domain.CustomerProfile;
import com.company.banking.auth.domain.CustomerProfileRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaCustomerProfileRepository implements CustomerProfileRepository {

    private final SpringDataCustomerRepository customers;

    public JpaCustomerProfileRepository(SpringDataCustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    @Transactional
    public CustomerProfile create(UUID userId, String fullName, String phone, String address) {
        Instant now = Instant.now();
        String customerNumber = nextCustomerNumber();
        CustomerJpaEntity entity = new CustomerJpaEntity(
                userId,
                customerNumber,
                fullName,
                phone,
                address,
                now
        );
        CustomerJpaEntity saved = customers.save(entity);
        return new CustomerProfile(
                saved.getId(),
                saved.getUserId(),
                saved.getCustomerNumber(),
                saved.getFullName(),
                saved.getPhone(),
                saved.getAddress(),
                saved.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String nextCustomerNumber() {
        int next = customers.findMaxCustomerSequence() + 1;
        return String.format("CUST-%06d", next);
    }
}
