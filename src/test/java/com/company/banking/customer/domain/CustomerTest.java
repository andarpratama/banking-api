package com.company.banking.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerTest {

    private static final UUID TEST_ID = UUID.randomUUID();
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String TEST_CUSTOMER_NUMBER = "CUST-000001";
    private static final String TEST_FULL_NAME = "John Doe";
    private static final String TEST_PHONE = "+1-555-0123";
    private static final String TEST_ADDRESS = "123 Main St";
    private static final Instant NOW = Instant.now();

    @Test
    void constructorValidatesRequiredFields() {
        assertThatThrownBy(() ->
                new Customer(null, TEST_USER_ID, TEST_CUSTOMER_NUMBER, TEST_FULL_NAME, TEST_PHONE, TEST_ADDRESS, CustomerStatus.ACTIVE, NOW, NOW)
        ).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                new Customer(TEST_ID, null, TEST_CUSTOMER_NUMBER, TEST_FULL_NAME, TEST_PHONE, TEST_ADDRESS, CustomerStatus.ACTIVE, NOW, NOW)
        ).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                new Customer(TEST_ID, TEST_USER_ID, null, TEST_FULL_NAME, TEST_PHONE, TEST_ADDRESS, CustomerStatus.ACTIVE, NOW, NOW)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createActiveCustomer() {
        Customer customer = new Customer(
                TEST_ID,
                TEST_USER_ID,
                TEST_CUSTOMER_NUMBER,
                TEST_FULL_NAME,
                TEST_PHONE,
                TEST_ADDRESS,
                CustomerStatus.ACTIVE,
                NOW,
                NOW
        );

        assertThat(customer.id()).isEqualTo(TEST_ID);
        assertThat(customer.userId()).isEqualTo(TEST_USER_ID);
        assertThat(customer.customerNumber()).isEqualTo(TEST_CUSTOMER_NUMBER);
        assertThat(customer.fullName()).isEqualTo(TEST_FULL_NAME);
        assertThat(customer.phone()).isEqualTo(TEST_PHONE);
        assertThat(customer.address()).isEqualTo(TEST_ADDRESS);
        assertThat(customer.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.isActive()).isTrue();
        assertThat(customer.isDeleted()).isFalse();
    }

    @Test
    void updateProfileReturnsNewInstance() {
        Customer original = new Customer(
                TEST_ID,
                TEST_USER_ID,
                TEST_CUSTOMER_NUMBER,
                TEST_FULL_NAME,
                TEST_PHONE,
                TEST_ADDRESS,
                CustomerStatus.ACTIVE,
                NOW,
                NOW
        );

        Instant later = NOW.plusSeconds(3600);
        Customer updated = original.updateProfile("Jane Doe", "+1-555-0456", "456 Oak Ave", later);

        assertThat(updated).isNotSameAs(original);
        assertThat(updated.fullName()).isEqualTo("Jane Doe");
        assertThat(updated.phone()).isEqualTo("+1-555-0456");
        assertThat(updated.address()).isEqualTo("456 Oak Ave");
        assertThat(updated.updatedAt()).isEqualTo(later);
        assertThat(updated.createdAt()).isEqualTo(original.createdAt());
        assertThat(updated.status()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void deleteReturnsNewSoftDeletedInstance() {
        Customer original = new Customer(
                TEST_ID,
                TEST_USER_ID,
                TEST_CUSTOMER_NUMBER,
                TEST_FULL_NAME,
                TEST_PHONE,
                TEST_ADDRESS,
                CustomerStatus.ACTIVE,
                NOW,
                NOW
        );

        Instant later = NOW.plusSeconds(3600);
        Customer deleted = original.delete(later);

        assertThat(deleted).isNotSameAs(original);
        assertThat(deleted.status()).isEqualTo(CustomerStatus.SOFT_DELETED);
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.isActive()).isFalse();
        assertThat(deleted.updatedAt()).isEqualTo(later);
    }

    @Test
    void phoneAndAddressCanBeNull() {
        Customer customer = new Customer(
                TEST_ID,
                TEST_USER_ID,
                TEST_CUSTOMER_NUMBER,
                TEST_FULL_NAME,
                null,
                null,
                CustomerStatus.ACTIVE,
                NOW,
                NOW
        );

        assertThat(customer.phone()).isNull();
        assertThat(customer.address()).isNull();
    }
}
