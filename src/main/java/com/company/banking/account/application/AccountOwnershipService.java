package com.company.banking.account.application;

import com.company.banking.account.domain.AccountRepository;
import com.company.banking.security.SecurityContextHelper;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Ownership checks for {@code @PreAuthorize} SpEL on account endpoints.
 */
@Service("accountOwnershipService")
public class AccountOwnershipService {

    private final AccountRepository accountRepository;
    private final SecurityContextHelper securityContextHelper;

    public AccountOwnershipService(
            AccountRepository accountRepository,
            SecurityContextHelper securityContextHelper
    ) {
        this.accountRepository = accountRepository;
        this.securityContextHelper = securityContextHelper;
    }

    /**
     * Returns true if the current user owns the account, or if the account does not exist
     * (so the application layer can return 404 instead of 403).
     */
    public boolean isOwner(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(account -> securityContextHelper.isOwner(account.customerId()))
                .orElse(true);
    }
}
