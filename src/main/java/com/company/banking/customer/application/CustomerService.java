package com.company.banking.customer.application;

import com.company.banking.customer.domain.Customer;
import com.company.banking.customer.domain.CustomerDomainException.CustomerNotFoundException;
import com.company.banking.customer.domain.CustomerRepository;
import com.company.banking.customer.domain.CustomerStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer application service — orchestrates domain operations.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    /**
     * Get customer by ID (admin or own profile).
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
        return customerMapper.toResponse(customer);
    }

    /**
     * List all active customers with pagination.
     */
    @Transactional(readOnly = true)
    public CustomerListResponse listCustomers(int page, int size, String sortBy, String sortDirection) {
        List<Customer> customers = customerRepository.findAllActive(page, size, sortBy, sortDirection);
        long total = customerRepository.countActive();
        
        List<CustomerResponse> content = customers.stream()
                .map(customerMapper::toResponse)
                .collect(Collectors.toList());
        
        int totalPages = (int) Math.ceil((double) total / size);
        
        return new CustomerListResponse(
                content,
                total,
                totalPages,
                page,
                size
        );
    }

    /**
     * Update customer profile.
     */
    @Transactional
    public CustomerResponse updateCustomer(UUID customerId, UpdateCustomerRequest request) {
        Customer existing = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
        
        Customer updated = existing.updateProfile(
                request.getFullName(),
                request.getPhone(),
                request.getAddress(),
                Instant.now()
        );
        
        Customer saved = customerRepository.save(updated);
        return customerMapper.toResponse(saved);
    }

    /**
     * Soft delete customer (admin only).
     */
    @Transactional
    public void deleteCustomer(UUID customerId) {
        Customer existing = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
        
        Customer deleted = existing.delete(Instant.now());
        customerRepository.save(deleted);
    }
}
