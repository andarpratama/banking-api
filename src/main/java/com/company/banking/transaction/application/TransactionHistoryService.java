package com.company.banking.transaction.application;

import com.company.banking.account.domain.Account;
import com.company.banking.account.domain.AccountRepository;
import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import com.company.banking.common.pagination.PageQuery;
import com.company.banking.common.pagination.PageResponse;
import com.company.banking.security.SecurityContextHelper;
import com.company.banking.transaction.domain.Transaction;
import com.company.banking.transaction.domain.TransactionRepository;
import com.company.banking.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction history use case — filtered, paginated ledger list (OpenAPI §5.1).
 */
@Service
public class TransactionHistoryService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt",
            "amount",
            "transactionType"
    );

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final SecurityContextHelper securityContextHelper;

    public TransactionHistoryService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper,
            SecurityContextHelper securityContextHelper
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.securityContextHelper = securityContextHelper;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getHistory(
            UUID accountId,
            Integer page,
            Integer size,
            String sort,
            TransactionType type,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount
    ) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found with ID: " + accountId
                ));

        if (!securityContextHelper.isAdmin()
                && !securityContextHelper.isOwner(account.customerId())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Cannot view another customer's transaction history"
            );
        }

        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "minAmount must be less than or equal to maxAmount"
            );
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "fromDate must be on or before toDate"
            );
        }

        PageQuery pageQuery = PageQuery.of(page, size, sort);
        if (!ALLOWED_SORT_PROPERTIES.contains(pageQuery.sort().property())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Invalid sort property; allowed: createdAt, amount, transactionType"
            );
        }

        Instant fromInstant = fromDate == null
                ? null
                : fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = toDate == null
                ? null
                : toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

        List<Transaction> transactions = transactionRepository.findByAccountFiltered(
                accountId,
                type,
                fromInstant,
                toInstant,
                minAmount,
                maxAmount,
                pageQuery.page(),
                pageQuery.size(),
                pageQuery.sort().property(),
                pageQuery.sort().direction().name()
        );
        long total = transactionRepository.countByAccountFiltered(
                accountId,
                type,
                fromInstant,
                toInstant,
                minAmount,
                maxAmount
        );

        List<TransactionResponse> content = transactions.stream()
                .map(transactionMapper::toResponse)
                .toList();

        return PageResponse.of(content, total, pageQuery);
    }
}
