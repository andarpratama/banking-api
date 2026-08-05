package com.company.banking.auth.domain;

import java.util.UUID;

public interface CustomerProfileRepository {

    CustomerProfile create(UUID userId, String fullName, String phone, String address);

    String nextCustomerNumber();
}
