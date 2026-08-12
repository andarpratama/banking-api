package com.company.banking.transaction.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for {@code POST /transactions/withdraw}.
 */
public class WithdrawRequest {

    @NotNull(message = "accountId is required")
    private UUID accountId;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @Size(max = 255, message = "description must be at most 255 characters")
    private String description;

    public WithdrawRequest() {
    }

    public WithdrawRequest(UUID accountId, BigDecimal amount, String description) {
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
