package com.company.banking.account.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Account repository port — defined in domain, implemented in infrastructure.
 */
public interface AccountRepository {

    Optional<Account> findById(UUID id);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(UUID customerId);

    long countByCustomerId(UUID customerId);

    Account save(Account account);

    /**
     * Next numeric sequence for account numbers ({@code ACC-0000001}).
     */
    int nextAccountSequence();
}
