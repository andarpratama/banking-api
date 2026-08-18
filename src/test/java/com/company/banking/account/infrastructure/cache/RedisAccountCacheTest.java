package com.company.banking.account.infrastructure.cache;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.banking.config.CacheNames;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class RedisAccountCacheTest {

    private CacheManager cacheManager;
    private Cache accountsCache;
    private RedisAccountCache redisAccountCache;

    @BeforeEach
    void setUp() {
        cacheManager = mock(CacheManager.class);
        accountsCache = mock(Cache.class);
        when(cacheManager.getCache(CacheNames.ACCOUNTS)).thenReturn(accountsCache);
        redisAccountCache = new RedisAccountCache(cacheManager);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void evictsImmediatelyWhenNoTransaction() {
        UUID accountId = UUID.randomUUID();

        redisAccountCache.evict(accountId);

        verify(accountsCache).evict(accountId);
    }

    @Test
    void defersEvictionUntilAfterCommit() {
        UUID accountId = UUID.randomUUID();
        TransactionSynchronizationManager.initSynchronization();

        redisAccountCache.evict(accountId);

        verify(accountsCache, never()).evict(accountId);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(accountsCache).evict(accountId);
    }

    @Test
    void ignoresNullAccountId() {
        redisAccountCache.evict(null);

        verify(cacheManager, never()).getCache(CacheNames.ACCOUNTS);
    }

    @Test
    void ignoresMissingCacheRegion() {
        when(cacheManager.getCache(CacheNames.ACCOUNTS)).thenReturn(null);

        redisAccountCache.evict(UUID.randomUUID());

        verify(accountsCache, never()).evict(org.mockito.ArgumentMatchers.any());
    }
}
