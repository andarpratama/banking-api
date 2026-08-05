package com.company.banking.customer.presentation;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer endpoints: list (admin only), get profile (admin or own).
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer management (ADMIN: list all, CUSTOMER: own profile only)")
public class CustomerController {

    /**
     * List all customers (ADMIN only).
     * GET /api/v1/customers
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "List all customers",
            description = "Requires ADMIN role. Returns paginated customer list."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customers list",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - not ADMIN"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - missing or invalid token"
            )
    })
    public ResponseEntity<String> listCustomers() {
        // TODO: implement pagination + filtering
        return ResponseEntity.ok("{\"customers\": []}");
    }

    /**
     * Get customer profile by ID (ADMIN or the customer themselves).
     * GET /api/v1/customers/{customerId}
     */
    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @securityContextHelper.getCurrentUsername() == #customerId")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get customer profile",
            description = "ADMIN can view any customer. CUSTOMER can view only their own profile."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer profile",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - no permission"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Customer not found"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    public ResponseEntity<String> getCustomerProfile(@PathVariable String customerId) {
        // TODO: implement get profile
        return ResponseEntity.ok("{\"customer\": {}}");
    }
}
