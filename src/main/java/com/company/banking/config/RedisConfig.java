package com.company.banking.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * Redis connection factory and template wiring for cache / token store use cases.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
        standalone.setHostName(redisProperties.getHost());
        standalone.setPort(redisProperties.getPort());
        standalone.setDatabase(redisProperties.getDatabase());
        if (StringUtils.hasText(redisProperties.getPassword())) {
            standalone.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                LettuceClientConfiguration.builder();
        if (redisProperties.getTimeout() != null) {
            clientBuilder.commandTimeout(redisProperties.getTimeout());
        }

        return new LettuceConnectionFactory(standalone, clientBuilder.build());
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
