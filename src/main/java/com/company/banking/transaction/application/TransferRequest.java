package com.company.banking.transaction.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for {@code POST /transactions/transfer}.
 */
public class TransferRequest {

    @NotNull(message = "sourceAccountId is required")
    private UUID sourceAccountId;

    @NotNull(message = "destinationAccountId is required")
    private UUID destinationAccountId;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

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
