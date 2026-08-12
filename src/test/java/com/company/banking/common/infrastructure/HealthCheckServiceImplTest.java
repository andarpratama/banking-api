package com.company.banking.common.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthCheckServiceImplTest {

    private HealthCheckServiceImpl healthCheckService;
    private JdbcTemplate jdbcTemplate;
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        healthCheckService = new HealthCheckServiceImpl(jdbcTemplate, stringRedisTemplate);
    }

    @Nested
    class CheckDatabaseHealth {

        @Test
        void returnsUpWhenDatabaseIsAccessible() {
            // Arrange
            when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                    .thenReturn(1);

            // Act
            String result = healthCheckService.checkDatabaseHealth();

            // Assert
            assertThat(result).isEqualTo("UP");
        }

        @Test
        void returnsDownWhenDatabaseThrowsException() {
            // Arrange
            when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                    .thenThrow(new RuntimeException("Connection failed"));

            // Act
            String result = healthCheckService.checkDatabaseHealth();

            // Assert
            assertThat(result).isEqualTo("DOWN");
        }
    }

    @Nested
    class CheckRedisHealth {

        @Test
        void returnsUpWhenRedisIsAccessible() {
            // Arrange
            RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
            RedisConnection connection = mock(RedisConnection.class);
            when(stringRedisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
            when(connectionFactory.getConnection()).thenReturn(connection);
            when(connection.ping()).thenReturn("PONG");

            // Act
            String result = healthCheckService.checkRedisHealth();

            // Assert
            assertThat(result).isEqualTo("UP");
        }

        @Test
        void returnsDownWhenRedisThrowsException() {
            // Arrange
            RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
            when(stringRedisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
            when(connectionFactory.getConnection()).thenThrow(new RuntimeException("Connection failed"));

            // Act
            String result = healthCheckService.checkRedisHealth();

            // Assert
            assertThat(result).isEqualTo("DOWN");
        }

        @Test
        void returnsUnknownWhenStringRedisTemplateIsNull() {
            // Arrange
            healthCheckService = new HealthCheckServiceImpl(jdbcTemplate, null);

            // Act
            String result = healthCheckService.checkRedisHealth();

            // Assert
            assertThat(result).isEqualTo("UNKNOWN");
        }
    }
}
