package com.company.banking.account.presentation;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account endpoints: create, get balance, freeze, close (with RBAC).
 * - ADMIN can freeze/close any account, view any account
 * - CUSTOMER can view only their own accounts, cannot freeze/close
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account management (create, balance, freeze, close)")
public class AccountController {

    /**
     * Create a new account for the authenticated customer.
     * POST /api/v1/accounts
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create account",
            description = "CUSTOMER creates account for themselves. ADMIN creates for any customer."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Account created",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    public ResponseEntity<String> createAccount() {
        // TODO: implement account creation
        return ResponseEntity.status(201).body("{\"account\": {}}");
    }

    /**
     * Get account balance.
     * GET /api/v1/accounts/{accountId}/balance
     */
    @GetMapping("/{accountId}/balance")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get account balance",
            description = "ADMIN can view any account balance. CUSTOMER can view only their own."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account balance",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - not owner"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    public ResponseEntity<String> getAccountBalance(@PathVariable String accountId) {
        // TODO: implement get balance
        return ResponseEntity.ok("{\"balance\": 0.00}");
    }

    /**
     * Freeze account (ADMIN only).
     * POST /api/v1/accounts/{accountId}/freeze
     */
    @PostMapping("/{accountId}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Freeze account",
            description = "Requires ADMIN role. Freezes an account to prevent transactions."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account frozen"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - not ADMIN"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    public ResponseEntity<String> freezeAccount(@PathVariable String accountId) {
        // TODO: implement freeze
        return ResponseEntity.ok("{\"status\": \"FROZEN\"}");
    }

    /**
     * Close account (ADMIN only).
     * POST /api/v1/accounts/{accountId}/close
     */
    @PostMapping("/{accountId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Close account",
            description = "Requires ADMIN role. Closes an account permanently."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account closed"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - not ADMIN"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    public ResponseEntity<String> closeAccount(@PathVariable String accountId) {
        // TODO: implement close
        return ResponseEntity.ok("{\"status\": \"CLOSED\"}");
    }
}
