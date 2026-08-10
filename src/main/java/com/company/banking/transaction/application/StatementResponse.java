package com.company.banking.transaction.application;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Account statement response aligned with OpenAPI §5.2.
 */
public class StatementResponse {

    private UUID accountId;
    private String accountNumber;
    private StatementPeriod statementPeriod;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private List<StatementTransactionItem> transactions;

    public StatementResponse() {
    }

    public StatementResponse(
            UUID accountId,
            String accountNumber,
            StatementPeriod statementPeriod,
            BigDecimal openingBalance,
            BigDecimal closingBalance,
            BigDecimal totalDeposits,
            BigDecimal totalWithdrawals,
            BigDecimal totalDebits,
            BigDecimal totalCredits,
            List<StatementTransactionItem> transactions
    ) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.statementPeriod = statementPeriod;
        this.openingBalance = openingBalance;
        this.closingBalance = closingBalance;
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.transactions = transactions;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public StatementPeriod getStatementPeriod() {
        return statementPeriod;
    }

    public void setStatementPeriod(StatementPeriod statementPeriod) {
        this.statementPeriod = statementPeriod;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
    }

    public BigDecimal getTotalDeposits() {
        return totalDeposits;
    }

    public void setTotalDeposits(BigDecimal totalDeposits) {
        this.totalDeposits = totalDeposits;
    }

    public BigDecimal getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public void setTotalWithdrawals(BigDecimal totalWithdrawals) {
        this.totalWithdrawals = totalWithdrawals;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public void setTotalDebits(BigDecimal totalDebits) {
        this.totalDebits = totalDebits;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits) {
        this.totalCredits = totalCredits;
    }

    public List<StatementTransactionItem> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<StatementTransactionItem> transactions) {
        this.transactions = transactions;
    }

    public static class StatementPeriod {

        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
                timezone = "UTC"
        )
        private Instant from;

        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
                timezone = "UTC"
        )
        private Instant to;

        public StatementPeriod() {
        }

        public StatementPeriod(Instant from, Instant to) {
            this.from = from;
            this.to = to;
        }

        public Instant getFrom() {
            return from;
        }

        public void setFrom(Instant from) {
            this.from = from;
        }

        public Instant getTo() {
            return to;
        }

        public void setTo(Instant to) {
            this.to = to;
        }
    }

    public static class StatementTransactionItem {

        private UUID id;

        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
                timezone = "UTC"
        )
        private Instant date;

        private String type;
        private BigDecimal amount;
        private BigDecimal balance;
        private String description;

        public StatementTransactionItem() {
        }

        public StatementTransactionItem(
                UUID id,
                Instant date,
                String type,
                BigDecimal amount,
                BigDecimal balance,
                String description
        ) {
            this.id = id;
            this.date = date;
            this.type = type;
            this.amount = amount;
            this.balance = balance;
            this.description = description;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public Instant getDate() {
            return date;
        }

        public void setDate(Instant date) {
            this.date = date;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
