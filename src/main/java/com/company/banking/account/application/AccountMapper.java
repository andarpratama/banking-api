package com.company.banking.account.application;

import com.company.banking.account.domain.Account;
import org.springframework.stereotype.Component;

/**
 * Maps Account domain entity to API response DTOs.
 */
@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.id(),
                account.accountNumber(),
                account.customerId(),
                account.accountType().name(),
                account.currency(),
                account.balance().amount(),
                account.status().name(),
                account.version(),
                account.createdAt(),
                account.updatedAt()
        );
    }

    public AccountStatusResponse toStatusResponse(Account account) {
        return new AccountStatusResponse(
                account.id(),
                account.accountNumber(),
                account.status().name(),
                account.updatedAt()
        );
    }
}
