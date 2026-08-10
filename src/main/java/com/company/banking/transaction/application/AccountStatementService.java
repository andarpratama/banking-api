package com.company.banking.transaction.application;

import com.company.banking.account.domain.Account;
import com.company.banking.account.domain.AccountRepository;
import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.common.money.Money;
import com.company.banking.security.SecurityContextHelper;
import com.company.banking.transaction.application.StatementResponse.StatementPeriod;
import com.company.banking.transaction.application.StatementResponse.StatementTransactionItem;
import com.company.banking.transaction.domain.Transaction;
import com.company.banking.transaction.domain.TransactionRepository;
import com.company.banking.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account statement use case — period summary + ledger lines (OpenAPI §5.2).
 */
@Service
public class AccountStatementService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityContextHelper securityContextHelper;

    public AccountStatementService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            SecurityContextHelper securityContextHelper
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.securityContextHelper = securityContextHelper;
    }

    @Transactional(readOnly = true)
    public StatementResponse getStatement(UUID accountId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "fromDate and toDate are required"
            );
        }
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "fromDate must be on or before toDate"
            );
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found with ID: " + accountId
                ));

        if (!securityContextHelper.isAdmin()
                && !securityContextHelper.isOwner(account.customerId())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Cannot view another customer's account statement"
            );
        }

        Instant fromInstant = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

        Money openingBalance = transactionRepository.findBalanceAfterBefore(accountId, fromInstant)
                .orElse(Money.zero());

        List<Transaction> periodTransactions = transactionRepository.findByAccountInPeriod(
                accountId,
                fromInstant,
                toInstant
        );

        Map<TransactionType, BigDecimal> totals = new EnumMap<>(TransactionType.class);
        for (TransactionType type : TransactionType.values()) {
            totals.put(type, Money.zero().amount());
        }

        List<StatementTransactionItem> items = new ArrayList<>(periodTransactions.size());
        for (Transaction tx : periodTransactions) {
            totals.merge(
                    tx.transactionType(),
                    tx.amount().amount(),
                    BigDecimal::add
            );
            items.add(new StatementTransactionItem(
                    tx.id(),
                    tx.createdAt(),
                    tx.transactionType().name(),
                    tx.amount().amount(),
                    tx.balanceAfter().amount(),
                    tx.description()
            ));
        }

        Money closingBalance = periodTransactions.isEmpty()
                ? openingBalance
                : periodTransactions.getLast().balanceAfter();

        return new StatementResponse(
                account.id(),
                account.accountNumber(),
                new StatementPeriod(fromInstant, toInstant),
                openingBalance.amount(),
                closingBalance.amount(),
                totals.get(TransactionType.DEPOSIT),
                totals.get(TransactionType.WITHDRAW),
                totals.get(TransactionType.DEBIT),
                totals.get(TransactionType.CREDIT),
                items
        );
    }
}
