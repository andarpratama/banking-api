package com.company.banking.common.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public liveness endpoint for local and Docker smoke checks.
 * <p>
 * Must remain on the security whitelist when JWT filter chain is added (T-020).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Liveness / smoke checks")
public class HealthController {

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(
            summary = "Health check",
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
}
