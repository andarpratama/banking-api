package com.company.banking.common.presentation;

/**
 * Readiness probe response with detailed dependency statuses.
 *
 * <p>
 * Used by K8s readiness probes to determine if the application
 * is ready to accept traffic.
 * </p>
 *
 * @param status overall status ("UP" or "DOWN")
 * @param database database connectivity status
 * @param cache Redis cache connectivity status
 */
public record HealthStatus(String status, String database, String cache) {
}
