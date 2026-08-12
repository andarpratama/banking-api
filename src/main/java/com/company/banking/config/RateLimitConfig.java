package com.company.banking.config;

import com.company.banking.security.InMemoryRateLimiter;
import com.company.banking.security.RateLimitFilter;
import com.company.banking.security.RateLimiter;
import com.company.banking.security.RedisRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.rate-limit", name = "backend", havingValue = "redis", matchIfMissing = true)
    public RateLimiter redisRateLimiter(StringRedisTemplate stringRedisTemplate) {
        return new RedisRateLimiter(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.rate-limit", name = "backend", havingValue = "memory")
    public RateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }

    @Bean
    public RateLimitFilter rateLimitFilter(
            RateLimiter rateLimiter,
            RateLimitProperties rateLimitProperties,
            ObjectMapper objectMapper
    ) {
        return new RateLimitFilter(rateLimiter, rateLimitProperties, objectMapper);
    }
}
