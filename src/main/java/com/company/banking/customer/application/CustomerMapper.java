package com.company.banking.customer.application;

import com.company.banking.customer.domain.Customer;
import com.company.banking.auth.infrastructure.persistence.SpringDataUserRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Maps between Customer domain entity and CustomerResponse DTO.
 */
@Component
public class CustomerMapper {

    private final SpringDataUserRepository userRepository;

    public CustomerMapper(SpringDataUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Convert Customer domain entity to CustomerResponse DTO.
     * Fetches email from user entity based on userId.
     */
    public CustomerResponse toResponse(Customer customer) {
        String email = userRepository.findById(customer.userId())
                .map(userEntity -> userEntity.getEmail())
                .orElse(null);

        return new CustomerResponse(
                customer.id(),
                customer.userId(),
                customer.customerNumber(),
                customer.fullName(),
                email,
                customer.phone(),
                customer.address(),
                customer.status().name(),
                customer.createdAt(),
                customer.updatedAt()
        );
    }
}
