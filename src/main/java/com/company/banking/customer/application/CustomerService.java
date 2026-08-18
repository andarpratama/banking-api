package com.company.banking.customer.application;

import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.config.CacheNames;
import com.company.banking.customer.domain.Customer;
import com.company.banking.customer.domain.CustomerRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(cacheNames = CacheNames.CUSTOMERS, key = "#customerId")
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "Customer not found with ID: " + customerId));
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
    @CachePut(cacheNames = CacheNames.CUSTOMERS, key = "#customerId")
    @Transactional
    public CustomerResponse updateCustomer(UUID customerId, UpdateCustomerRequest request) {
        Customer existing = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "Customer not found with ID: " + customerId));
        
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
    @CacheEvict(cacheNames = CacheNames.CUSTOMERS, key = "#customerId")
    @Transactional
    public void deleteCustomer(UUID customerId) {
        Customer existing = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "Customer not found with ID: " + customerId));
        
        Customer deleted = existing.delete(Instant.now());
        customerRepository.save(deleted);
    }
}
