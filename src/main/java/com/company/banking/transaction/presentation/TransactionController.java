package com.company.banking.transaction.presentation;

import com.company.banking.transaction.application.DepositRequest;
import com.company.banking.transaction.application.DepositService;
import com.company.banking.transaction.application.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transaction endpoints — deposit (OpenAPI §4.1).
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Deposit, withdraw, and transfer")
public class TransactionController {

    private final DepositService depositService;

    public TransactionController(DepositService depositService) {
        this.depositService = depositService;
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Deposit funds",
            description = "Credits an ACTIVE account and inserts an immutable DEPOSIT ledger row. "
                    + "CUSTOMER may deposit to own accounts only; ADMIN may deposit to any account."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Deposit completed",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid amount or validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not owner"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Account frozen or closed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(depositService.deposit(request));
    }
}
