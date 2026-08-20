package com.company.banking.dashboard.presentation;

import com.company.banking.common.presentation.OpenApiExamples;
import com.company.banking.common.response.ErrorResponse;
import com.company.banking.dashboard.application.DashboardMetricsResponse;
import com.company.banking.dashboard.application.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard analytics endpoints (ADMIN only) — OpenAPI §6.1.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Dashboard analytics (ADMIN only)")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Get dashboard metrics (ADMIN only).
     * GET /api/v1/dashboard/metrics
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get dashboard metrics",
            description = "Requires ADMIN role. Returns system-wide customer/account counts, "
                    + "total balance, and daily/weekly deposit, withdrawal, and transfer volumes. "
                    + "Transfer volumes use DEBIT ledger legs only (no double-count with CREDIT)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dashboard metrics",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DashboardMetricsResponse.class),
                            examples = @ExampleObject(
                                    name = "System metrics",
                                    value = OpenApiExamples.DASHBOARD_METRICS_RESPONSE
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - not ADMIN",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Forbidden", value = OpenApiExamples.ERROR_FORBIDDEN)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "Unauthorized", value = OpenApiExamples.ERROR_UNAUTHORIZED)
                    )
            )
    })
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics() {
        return ResponseEntity.ok(dashboardService.getMetrics());
    }
}
