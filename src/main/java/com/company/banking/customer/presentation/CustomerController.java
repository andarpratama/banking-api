package com.company.banking.customer.presentation;

import com.company.banking.customer.application.CustomerListResponse;
import com.company.banking.customer.application.CustomerResponse;
import com.company.banking.customer.application.CustomerService;
import com.company.banking.customer.application.UpdateCustomerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer endpoints: list (admin only), get profile (admin or own), update, delete.
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer management (ADMIN: list all, CUSTOMER: own profile only)")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * List all customers (ADMIN only) with pagination.
     * GET /api/v1/customers?page=0&size=20&sort=createdAt,desc
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
                    content = @Content(schema = @Schema(implementation = CustomerListResponse.class))
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
    public ResponseEntity<CustomerListResponse> listCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        CustomerListResponse response = customerService.listCustomers(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    /**
     * Get customer profile by ID (ADMIN or the customer themselves).
     * GET /api/v1/customers/{customerId}
     */
    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @securityContextHelper.isOwner(#customerId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get customer profile",
            description = "ADMIN can view any customer. CUSTOMER can view only their own profile."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer profile",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))
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
    public ResponseEntity<CustomerResponse> getCustomerProfile(@PathVariable UUID customerId) {
        CustomerResponse response = customerService.getCustomer(customerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update customer profile (ADMIN or the customer themselves).
     * PUT /api/v1/customers/{customerId}
     */
    @PutMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @securityContextHelper.isOwner(#customerId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update customer profile",
            description = "ADMIN can update any customer. CUSTOMER can update only their own profile."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Customer updated successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - validation failed"
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
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        CustomerResponse response = customerService.updateCustomer(customerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft delete customer (ADMIN only).
     * DELETE /api/v1/customers/{customerId}
     */
    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Soft delete customer",
            description = "Requires ADMIN role. Marks customer as soft-deleted."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Customer soft-deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - not ADMIN"
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
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}
