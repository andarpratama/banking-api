package com.company.banking.common.presentation;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

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
@JsonPropertyOrder({"status", "database", "cache"})
@Schema(description = "Readiness probe payload with dependency statuses")
public record HealthStatus(
        @Schema(example = "UP", description = "Overall readiness")
        String status,
        @Schema(example = "UP", description = "PostgreSQL connectivity")
        String database,
        @Schema(example = "UP", description = "Redis connectivity")
        String cache
) {
}
