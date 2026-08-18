package com.company.banking.account.application;

import java.util.UUID;

/**
 * Evicts cached account reads after status or balance mutations.
 */
public interface AccountCache {

    void evict(UUID accountId);
}
