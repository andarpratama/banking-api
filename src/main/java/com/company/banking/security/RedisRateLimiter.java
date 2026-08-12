package com.company.banking.security;

import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis fixed-window rate limiter (multi-instance safe).
 */
public class RedisRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "rate:";

    private static final DefaultRedisScript<Long> INCR_SCRIPT = new DefaultRedisScript<>();

    static {
        INCR_SCRIPT.setResultType(Long.class);
        INCR_SCRIPT.setScriptText(
                """
                local current = redis.call('INCR', KEYS[1])
                if current == 1 then
                  redis.call('EXPIRE', KEYS[1], ARGV[1])
                end
                return current
                """
        );
    }

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public RateLimitResult tryConsume(String key, int limit, int windowSeconds) {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        long windowStart = (nowSeconds / windowSeconds) * windowSeconds;
        long resetEpochSeconds = windowStart + windowSeconds;
        String redisKey = KEY_PREFIX + key + ":" + windowStart;

        Long count = redis.execute(
                INCR_SCRIPT,
                List.of(redisKey),
                String.valueOf(windowSeconds)
        );
        long used = count == null ? 1L : count;
        boolean allowed = used <= limit;
        int remaining = (int) Math.max(0, limit - used);
        return new RateLimitResult(allowed, limit, allowed ? remaining : 0, resetEpochSeconds);
    }
}
