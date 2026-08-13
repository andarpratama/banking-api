package com.company.banking.customer.infrastructure.persistence;

import com.company.banking.auth.infrastructure.persistence.CustomerJpaEntity;
import com.company.banking.customer.domain.Customer;
import com.company.banking.customer.domain.CustomerRepository;
import com.company.banking.customer.domain.CustomerStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/**
 * JPA implementation of CustomerRepository.
 * Maps between JPA entity and domain Customer.
 */
@Repository
public class JpaCustomerRepository implements CustomerRepository {

    private final SpringDataCustomerFeatureRepository springDataRepository;

    public JpaCustomerRepository(SpringDataCustomerFeatureRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Customer> findByCustomerNumber(String customerNumber) {
        return springDataRepository.findByCustomerNumber(customerNumber).map(this::toDomain);
    }

    @Override
    public Optional<Customer> findByUserId(UUID userId) {
        return springDataRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity entity = springDataRepository.findById(customer.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot persist unknown customer " + customer.id()));
        entity.applyProfile(
                customer.fullName(),
                customer.phone(),
                customer.address(),
                customer.updatedAt()
        );
        entity.applyLifecycle(
                customer.status().name(),
                customer.isDeleted(),
                customer.updatedAt()
        );
        return toDomain(springDataRepository.save(entity));
    }

    @Override
    public List<Customer> findAllActive(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<CustomerJpaEntity> result = springDataRepository.findAllActive(pageable);
        return result.getContent().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countActive() {
        return springDataRepository.countActive();
    }

    private Customer toDomain(CustomerJpaEntity entity) {
        return new Customer(
                entity.getId(),
                entity.getUserId(),
                entity.getCustomerNumber(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getAddress(),
                CustomerStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
