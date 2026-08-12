package com.company.banking.common.application;

/**
 * Health check service abstraction for database and cache connectivity.
 *
 * <p>
 * Separates health checking logic from presentation layer,
 * enabling independent testing and reuse across features.
 * </p>
 */
public interface HealthCheckService {

    /**
     * Check database connectivity.
     *
     * @return "UP" if database is accessible, "DOWN" otherwise
     */
    String checkDatabaseHealth();

    /**
     * Check Redis cache connectivity.
     *
     * @return "UP" if Redis is accessible, "DOWN" or "UNKNOWN" otherwise
     */
    String checkRedisHealth();
}
