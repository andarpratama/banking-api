package com.company.banking.dashboard.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard analytics endpoints (ADMIN only).
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Dashboard analytics (ADMIN only)")
public class DashboardController {

    /**
     * Get dashboard metrics (ADMIN only).
     * GET /api/v1/dashboard/metrics
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get dashboard metrics",
            description = "Requires ADMIN role. Returns system-wide metrics: total customers, total balance, etc."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dashboard metrics",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - not ADMIN"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    public ResponseEntity<String> getDashboardMetrics() {
        // TODO: implement dashboard metrics calculation
        return ResponseEntity.ok("{\"metrics\": {}}");
    }
}
