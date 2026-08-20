package com.company.banking.transaction.presentation;

import com.company.banking.common.presentation.OpenApiExamples;
import com.company.banking.common.response.ErrorResponse;
import com.company.banking.transaction.application.AccountStatementService;
import com.company.banking.transaction.application.StatementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Account statement endpoint — OpenAPI §5.2.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Statement", description = "Period statement with balances and ledger lines")
public class AccountStatementController {

    private final AccountStatementService accountStatementService;

    public AccountStatementController(AccountStatementService accountStatementService) {
        this.accountStatementService = accountStatementService;
    }

    @GetMapping("/{accountId}/statement")
    @PreAuthorize("hasRole('ADMIN') or @accountOwnershipService.isOwner(#accountId)")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get statement for account",
            description = "Returns opening/closing balances, type totals, and ledger lines "
                    + "for the inclusive UTC date range. ADMIN can view any account; "
                    + "CUSTOMER only their own. fromDate and toDate are required."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account statement",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = StatementResponse.class),
                            examples = @ExampleObject(
                                    name = "Period statement",
                                    value = OpenApiExamples.STATEMENT_RESPONSE
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Missing or invalid date range"),
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
    public ResponseEntity<StatementResponse> getStatement(
            @PathVariable UUID accountId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(accountStatementService.getStatement(accountId, fromDate, toDate));
    }
}
