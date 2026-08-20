package com.company.banking.transaction.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for {@code POST /transactions/deposit}.
 */
@Schema(description = "Deposit funds payload")
public class DepositRequest {

    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    @NotNull(message = "accountId is required")
    private UUID accountId;

    @Schema(example = "500.00")
    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @Schema(example = "Cash deposit at ATM")
    @Size(max = 255, message = "description must be at most 255 characters")
    private String description;

    public DepositRequest() {
    }

    public DepositRequest(UUID accountId, BigDecimal amount, String description) {
        this.accountId = accountId;
        this.amount = amount;
        this.description = description;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
