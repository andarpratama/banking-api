package com.company.banking.account.application;

import com.company.banking.account.domain.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for {@code POST /accounts}.
 */
@Schema(description = "Create account payload")
public class CreateAccountRequest {

    @Schema(example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    @NotNull(message = "customerId is required")
    private UUID customerId;

    @Schema(example = "SAVINGS")
    @NotNull(message = "accountType is required")
    private AccountType accountType;

    @Schema(example = "USD")
    @NotNull(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter ISO code")
    private String currency;

    @Schema(example = "1000.00")
    @DecimalMin(value = "0.00", inclusive = true, message = "initialBalance must not be negative")
    private BigDecimal initialBalance;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(
            UUID customerId,
            AccountType accountType,
            String currency,
            BigDecimal initialBalance
    ) {
        this.customerId = customerId;
        this.accountType = accountType;
        this.currency = currency;
        this.initialBalance = initialBalance;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
}
