package com.company.banking.common.presentation;

import com.company.banking.common.application.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * K8s-aware health check endpoints for liveness and readiness probes.
 * <p>
 * Must remain on the security whitelist when JWT filter chain is added (T-020).
 * <p>
 * Endpoints:
 * - /api/v1/health — Overall status (legacy, simple)
 * - /api/v1/health/live — Liveness probe (app process running?)
 * - /api/v1/health/ready — Readiness probe (dependencies healthy?)
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Liveness / readiness probes for Kubernetes")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final HealthCheckService healthCheckService;

    public HealthController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(
            summary = "Health check (legacy)",
            description = "Public liveness probe. Returns status UP when the application process is running."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Application is up",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = HealthResponse.class)
            )
    )
    public HealthResponse health() {
        return HealthResponse.up();
    }

    @GetMapping(value = "/health/live", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(
            summary = "Liveness probe for Kubernetes",
            description = "Indicates whether the application process is running. Used by K8s to decide if pod should be restarted. Fast check (no dependency checks)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Application is alive",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = HealthResponse.class)
            )
    )
    public ResponseEntity<HealthResponse> liveness() {
        return ResponseEntity.ok(HealthResponse.up());
    }

    @GetMapping(value = "/health/ready", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(
            summary = "Readiness probe for Kubernetes",
            description = "Indicates whether the application is ready to accept traffic. Checks database and cache connectivity. Used by K8s load balancer."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Application is ready (all dependencies healthy)",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = HealthStatus.class)
            )
    )
    @ApiResponse(
            responseCode = "503",
            description = "Application is not ready (one or more dependencies unhealthy)"
    )
    public ResponseEntity<?> readiness() {
        String dbStatus = healthCheckService.checkDatabaseHealth();
        String cacheStatus = healthCheckService.checkRedisHealth();

        HealthStatus healthStatus = new HealthStatus(
                (dbStatus.equals("UP") && cacheStatus.equals("UP")) ? "UP" : "DOWN",
                dbStatus,
                cacheStatus
        );

        if (healthStatus.status().equals("UP")) {
            return ResponseEntity.ok(healthStatus);
        } else {
            log.warn("Readiness check failed: db={}, redis={}", dbStatus, cacheStatus);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthStatus);
        }
    }
}
