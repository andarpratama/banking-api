package com.company.banking.account.presentation;

import com.company.banking.account.application.AccountListResponse;
import com.company.banking.account.application.AccountResponse;
import com.company.banking.account.application.AccountService;
import com.company.banking.account.application.AccountStatusResponse;
import com.company.banking.account.application.CreateAccountRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account endpoints aligned with OpenAPI §3 (create, list, get/balance, freeze, unfreeze, close).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Accounts", description = "Account management (create, balance, freeze, unfreeze, close)")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create account",
            description = "CUSTOMER creates an account for themselves. ADMIN may create for any customer."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Account created",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/customers/{customerId}/accounts")
    @PreAuthorize("hasRole('ADMIN') or @securityContextHelper.isOwner(#customerId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "List accounts for customer",
            description = "ADMIN can list any customer's accounts. CUSTOMER can list only their own."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Accounts list",
                    content = @Content(schema = @Schema(implementation = AccountListResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AccountListResponse> listAccountsForCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(accountService.listAccountsForCustomer(customerId));
    }

    @GetMapping("/accounts/{accountId}")
    @PreAuthorize("hasRole('ADMIN') or @accountOwnershipService.isOwner(#accountId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get account by ID",
            description = "Returns account details including balance and optimistic-lock version. "
                    + "ADMIN can view any account; CUSTOMER only their own."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account details (includes balance)",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "Forbidden - not owner"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

    @PatchMapping("/accounts/{accountId}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Freeze account",
            description = "Requires ADMIN. Transitions ACTIVE → FROZEN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account frozen",
                    content = @Content(schema = @Schema(implementation = AccountStatusResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "Forbidden - not ADMIN"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AccountStatusResponse> freezeAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.freezeAccount(accountId));
    }

    @PatchMapping("/accounts/{accountId}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Unfreeze account",
            description = "Requires ADMIN. Transitions FROZEN → ACTIVE only."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account unfrozen",
                    content = @Content(schema = @Schema(implementation = AccountStatusResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "Forbidden - not ADMIN"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition (e.g. closed)"),
            @ApiResponse(responseCode = "400", description = "Account is not frozen"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AccountStatusResponse> unfreezeAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.unfreezeAccount(accountId));
    }

    @PatchMapping("/accounts/{accountId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Close account",
            description = "Requires ADMIN. Transitions ACTIVE → CLOSED (final)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account closed",
                    content = @Content(schema = @Schema(implementation = AccountStatusResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "Forbidden - not ADMIN"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AccountStatusResponse> closeAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.closeAccount(accountId));
    }
}
