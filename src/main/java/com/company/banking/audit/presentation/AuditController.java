package com.company.banking.audit.presentation;

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
 * Audit log endpoints (ADMIN only).
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Audit logs (ADMIN only)")
public class AuditController {

    /**
     * List audit logs (ADMIN only).
     * GET /api/v1/audit/logs
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "List audit logs",
            description = "Requires ADMIN role. Returns paginated audit log entries."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Audit logs",
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
    public ResponseEntity<String> listAuditLogs() {
        // TODO: implement audit log listing with filters/pagination
        return ResponseEntity.ok("{\"logs\": []}");
    }
}
