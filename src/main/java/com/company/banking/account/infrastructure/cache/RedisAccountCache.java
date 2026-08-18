package com.company.banking.account.infrastructure.cache;

import com.company.banking.account.application.AccountCache;
import com.company.banking.config.CacheNames;
import java.util.UUID;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Evicts the {@code accounts} cache after the surrounding transaction commits,
 * so readers cannot refill the cache from an uncommitted row.
 */
@Component
public class RedisAccountCache implements AccountCache {

    private final CacheManager cacheManager;

    public RedisAccountCache(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void evict(UUID accountId) {
        if (accountId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictNow(accountId);
                }
            });
            return;
        }
        evictNow(accountId);
    }

    private void evictNow(UUID accountId) {
        Cache cache = cacheManager.getCache(CacheNames.ACCOUNTS);
        if (cache != null) {
            cache.evict(accountId);
        }
    }
}
