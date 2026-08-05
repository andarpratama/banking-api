package com.company.banking.account.application;

import org.springframework.stereotype.Service;

/**
 * Service for checking if a user owns an account.
 * Used by @PreAuthorize SpEL expressions to enforce ownership-based access control.
 */
@Service
public class AccountOwnershipService {

    /**
     * Check if the given username (email) owns the given account.
     *
     * @param accountId the account ID to check
     * @param username  the username/email of the user
     * @return true if the user owns the account, false otherwise
     */
    public boolean isOwner(String accountId, String username) {
        // TODO: implement ownership check by looking up account and comparing with username
        // For now, return false to be safe
        return false;
    }
}
