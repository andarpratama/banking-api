package com.company.banking.common.presentation;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Health status response for K8s readiness probe.
 * Includes individual component statuses (database, cache).
 */
@JsonPropertyOrder({"status", "database", "cache"})
public record HealthStatus(
        String status,
        String database,
        String cache
) {
}
