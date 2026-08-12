package com.company.banking.common.infrastructure;

import com.company.banking.common.application.HealthCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Implementation of health check service.
 *
 * <p>
 * Handles database and cache connectivity checks for readiness probes.
 * Logs failures but does not throw exceptions — allows graceful degradation.
 * </p>
 */
@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public HealthCheckServiceImpl(JdbcTemplate jdbcTemplate, StringRedisTemplate stringRedisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String checkDatabaseHealth() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            log.warn("Database health check failed", e);
            return "DOWN";
        }
    }

    @Override
    public String checkRedisHealth() {
        if (stringRedisTemplate == null) {
            return "UNKNOWN";
        }
        try {
            stringRedisTemplate.getConnectionFactory().getConnection().ping();
            return "UP";
        } catch (Exception e) {
            log.warn("Redis health check failed", e);
            return "DOWN";
        }
    }
}
