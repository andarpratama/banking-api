package com.company.banking.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.banking.customer.domain.Customer;
import com.company.banking.customer.domain.CustomerDomainException.CustomerNotFoundException;
import com.company.banking.customer.domain.CustomerRepository;
import com.company.banking.customer.domain.CustomerStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerServiceTest {

    private CustomerRepository customerRepository;
    private CustomerMapper customerMapper;
    private CustomerService customerService;

    private UUID customerId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerMapper = mock(CustomerMapper.class);
        customerService = new CustomerService(customerRepository, customerMapper);

        customerId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void getCustomerReturnsResponseWhenFound() {
        Customer customer = createTestCustomer();
        CustomerResponse expectedResponse = new CustomerResponse();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(expectedResponse);

        CustomerResponse result = customerService.getCustomer(customerId);

        assertThat(result).isEqualTo(expectedResponse);
        verify(customerRepository).findById(customerId);
        verify(customerMapper).toResponse(customer);
    }

    @Test
    void getCustomerThrowsNotFoundWhenMissing() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(customerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void listCustomersReturnsPaginatedResults() {
        Customer customer1 = createTestCustomer();
        Customer customer2 = createTestCustomer();
        List<Customer> customers = List.of(customer1, customer2);

        CustomerResponse response1 = new CustomerResponse();
        CustomerResponse response2 = new CustomerResponse();

        when(customerRepository.findAllActive(0, 20, "createdAt", "DESC")).thenReturn(customers);
        when(customerRepository.countActive()).thenReturn(2L);
        when(customerMapper.toResponse(customer1)).thenReturn(response1);
        when(customerMapper.toResponse(customer2)).thenReturn(response2);

        CustomerListResponse result = customerService.listCustomers(0, 20, "createdAt", "DESC");

        assertThat(result.getContent()).hasSize(2).containsExactly(response1, response2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(20);
    }

    @Test
    void updateCustomerChangesProfile() {
        Customer existing = createTestCustomer();
        UpdateCustomerRequest request = new UpdateCustomerRequest("Jane Doe", "+1-555-0456", "456 Oak Ave");
        Customer updated = existing.updateProfile(request.getFullName(), request.getPhone(), request.getAddress(), Instant.now());
        CustomerResponse expectedResponse = new CustomerResponse();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenReturn(updated);
        when(customerMapper.toResponse(updated)).thenReturn(expectedResponse);

        CustomerResponse result = customerService.updateCustomer(customerId, request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void deleteCustomerSoftDeletesRecord() {
        Customer existing = createTestCustomer();
        Customer deleted = existing.delete(Instant.now());

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenReturn(deleted);

        customerService.deleteCustomer(customerId);

        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(any(Customer.class));
    }

    private Customer createTestCustomer() {
        return new Customer(
                customerId,
                userId,
                "CUST-000001",
                "John Doe",
                "+1-555-0123",
                "123 Main St",
                CustomerStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
    }
}
