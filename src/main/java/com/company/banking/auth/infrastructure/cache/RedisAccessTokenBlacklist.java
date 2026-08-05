package com.company.banking.auth.infrastructure.cache;

import com.company.banking.auth.domain.TokenHasher;
import com.company.banking.security.AccessTokenBlacklist;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed access-token revocation store (logout).
 */
@Component
public class RedisAccessTokenBlacklist implements AccessTokenBlacklist {

    private static final String KEY_PREFIX = "auth:access-blacklist:";

    private final StringRedisTemplate redis;

    public RedisAccessTokenBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void blacklist(String rawToken, Instant expiresAt) {
        Instant now = Instant.now();
        if (expiresAt.isBefore(now) || expiresAt.equals(now)) {
            return;
        }
        Duration ttl = Duration.between(now, expiresAt);
        redis.opsForValue().set(key(rawToken), "1", ttl);
    }

    @Override
    public boolean isBlacklisted(String rawToken) {
        Boolean present = redis.hasKey(key(rawToken));
        return Boolean.TRUE.equals(present);
    }

    private static String key(String rawToken) {
        return KEY_PREFIX + TokenHasher.sha256(rawToken);
    }
}
