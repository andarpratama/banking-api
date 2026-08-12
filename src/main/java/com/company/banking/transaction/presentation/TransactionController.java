package com.company.banking.transaction.presentation;

import com.company.banking.transaction.application.DepositRequest;
import com.company.banking.transaction.application.DepositService;
import com.company.banking.transaction.application.TransactionResponse;
import com.company.banking.transaction.application.TransferRequest;
import com.company.banking.transaction.application.TransferResponse;
import com.company.banking.transaction.application.TransferService;
import com.company.banking.transaction.application.WithdrawRequest;
import com.company.banking.transaction.application.WithdrawService;
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
 * Transaction endpoints — deposit, withdraw, and transfer (OpenAPI §4.1–§4.3).
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Deposit, withdraw, and transfer")
public class TransactionController {

    private final DepositService depositService;
    private final WithdrawService withdrawService;
    private final TransferService transferService;

    public TransactionController(
            DepositService depositService,
            WithdrawService withdrawService,
            TransferService transferService
    ) {
        this.depositService = depositService;
        this.withdrawService = withdrawService;
        this.transferService = transferService;
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

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Withdraw funds",
            description = "Debits an ACTIVE account with sufficient balance and inserts an immutable "
                    + "WITHDRAW ledger row. CUSTOMER may withdraw from own accounts only; "
                    + "ADMIN may withdraw from any account."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Withdraw completed",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid amount or validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not owner"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Insufficient balance, account frozen, or account closed"
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(withdrawService.withdraw(request));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Transfer funds",
            description = "Atomically debits the source account and credits the destination, "
                    + "inserting paired DEBIT/CREDIT ledger rows that share a referenceId. "
                    + "Account balances use optimistic locking (@Version); conflicts return 409 "
                    + "OPTIMISTIC_LOCK_EXCEPTION without server-side retry. "
                    + "CUSTOMER may transfer from own accounts only; ADMIN may transfer from any."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transfer completed",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid amount or validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not owner of source"),
            @ApiResponse(responseCode = "404", description = "Source or destination account not found"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Same-account transfer, insufficient balance, frozen/closed, "
                            + "or optimistic lock conflict"
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transferService.transfer(request));
    }
}
