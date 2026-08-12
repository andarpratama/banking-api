package com.company.banking.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

    @Test
    void allowsUntilLimitThenRejectsWithinSameWindow() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();

        RateLimitResult first = limiter.tryConsume("global:ip:1.1.1.1", 2, 60);
        RateLimitResult second = limiter.tryConsume("global:ip:1.1.1.1", 2, 60);
        RateLimitResult third = limiter.tryConsume("global:ip:1.1.1.1", 2, 60);

        assertThat(first.allowed()).isTrue();
        assertThat(first.remaining()).isEqualTo(1);
        assertThat(second.allowed()).isTrue();
        assertThat(second.remaining()).isEqualTo(0);
        assertThat(third.allowed()).isFalse();
        assertThat(third.remaining()).isEqualTo(0);
        assertThat(third.limit()).isEqualTo(2);
        assertThat(third.resetEpochSeconds()).isGreaterThan(0);
    }

    @Test
    void isolatesKeys() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter();

        assertThat(limiter.tryConsume("a", 1, 60).allowed()).isTrue();
        assertThat(limiter.tryConsume("b", 1, 60).allowed()).isTrue();
        assertThat(limiter.tryConsume("a", 1, 60).allowed()).isFalse();
    }
}
