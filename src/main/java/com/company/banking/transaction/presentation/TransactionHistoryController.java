package com.company.banking.transaction.presentation;

import com.company.banking.common.pagination.PageResponse;
import com.company.banking.common.presentation.OpenApiExamples;
import com.company.banking.common.response.ErrorResponse;
import com.company.banking.transaction.application.TransactionHistoryService;
import com.company.banking.transaction.application.TransactionResponse;
import com.company.banking.transaction.domain.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transaction history endpoint — OpenAPI §5.1.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Transaction History", description = "Paginated account transaction history")
public class TransactionHistoryController {

    private final TransactionHistoryService transactionHistoryService;

    public TransactionHistoryController(TransactionHistoryService transactionHistoryService) {
        this.transactionHistoryService = transactionHistoryService;
    }

    @GetMapping("/{accountId}/transactions")
    @PreAuthorize("hasRole('ADMIN') or @accountOwnershipService.isOwner(#accountId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get transactions for account",
            description = "Returns a paginated ledger history for the account. "
                    + "Supports filters: type, fromDate, toDate, minAmount, maxAmount. "
                    + "ADMIN can view any account; CUSTOMER only their own."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated transaction history",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PageResponse.class),
                            examples = @ExampleObject(
                                    name = "Transaction history",
                                    value = OpenApiExamples.TRANSACTION_HISTORY_RESPONSE
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid filter or pagination"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not owner"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Account not found",
                                    value = OpenApiExamples.ERROR_ACCOUNT_NOT_FOUND
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PageResponse<TransactionResponse>> getHistory(
            @PathVariable UUID accountId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount
    ) {
        return ResponseEntity.ok(transactionHistoryService.getHistory(
                accountId,
                page,
                size,
                sort,
                type,
                fromDate,
                toDate,
                minAmount,
                maxAmount
        ));
    }
}
