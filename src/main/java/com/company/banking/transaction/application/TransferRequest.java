package com.company.banking.transaction.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for {@code POST /transactions/transfer}.
 */
@Schema(description = "Transfer funds payload")
public class TransferRequest {

    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    @NotNull(message = "sourceAccountId is required")
    private UUID sourceAccountId;

    @Schema(example = "6b8e2c1a-4d5f-4a90-b3c1-9e7d6f5a4b3c")
    @NotNull(message = "destinationAccountId is required")
    private UUID destinationAccountId;

    @Schema(example = "250.00")
    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @Schema(example = "Transfer to friend")
    @Size(max = 255, message = "description must be at most 255 characters")
    private String description;

    public TransferRequest() {
    }

    public TransferRequest(
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String description
    ) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.description = description;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(UUID destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
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
