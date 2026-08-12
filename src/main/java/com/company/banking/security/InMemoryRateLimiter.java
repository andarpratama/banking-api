package com.company.banking.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Process-local fixed-window rate limiter (dev/test or single-instance).
 */
public class InMemoryRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, WindowCounter> windows = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryConsume(String key, int limit, int windowSeconds) {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long windowStart = (nowSeconds / windowSeconds) * windowSeconds;
        long resetEpochSeconds = windowStart + windowSeconds;
        String bucketKey = key + ":" + windowStart;

        WindowCounter counter = windows.compute(bucketKey, (k, existing) -> {
            if (existing == null) {
                return new WindowCounter(resetEpochSeconds);
            }
            return existing;
        });

        int count = counter.increment();
        pruneStale(nowSeconds);

        int remaining = Math.max(0, limit - count);
        boolean allowed = count <= limit;
        return new RateLimitResult(allowed, limit, allowed ? remaining : 0, resetEpochSeconds);
    }

    private void pruneStale(long nowSeconds) {
        if (windows.size() < 256) {
            return;
        }
        windows.entrySet().removeIf(e -> e.getValue().resetEpochSeconds <= nowSeconds);
    }

    private static final class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private final long resetEpochSeconds;

        private WindowCounter(long resetEpochSeconds) {
            this.resetEpochSeconds = resetEpochSeconds;
        }

        private int increment() {
            return count.incrementAndGet();
        }
    }
}
