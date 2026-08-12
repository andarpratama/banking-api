package com.company.banking.transaction.infrastructure.persistence;

import com.company.banking.common.money.Money;
import com.company.banking.transaction.domain.Transaction;
import com.company.banking.transaction.domain.TransactionRepository;
import com.company.banking.transaction.domain.TransactionType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/**
 * Insert-only JPA adapter for the transaction ledger, plus filtered history queries.
 */
@Repository
public class JpaTransactionRepository implements TransactionRepository {

    private static final Set<String> ALLOWED_SORT = Set.of("createdAt", "amount", "transactionType");
    private static final Map<String, String> SORT_TO_ENTITY = Map.of(
            "createdAt", "createdAt",
            "amount", "amount",
            "transactionType", "transactionType"
    );

    private final SpringDataTransactionRepository springDataRepository;

    public JpaTransactionRepository(SpringDataTransactionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        if (springDataRepository.existsById(transaction.id())) {
            throw new IllegalStateException("Ledger rows are immutable; cannot update transaction "
                    + transaction.id());
        }

        TransactionJpaEntity entity = new TransactionJpaEntity(
                transaction.id(),
                transaction.accountId(),
                transaction.referenceId(),
                transaction.transactionType().name(),
                transaction.amount().amount(),
                transaction.balanceAfter().amount(),
                transaction.description(),
                transaction.createdAt()
        );

        TransactionJpaEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Transaction> findByAccountFiltered(
            UUID accountId,
            TransactionType type,
            Instant fromDate,
            Instant toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Specification<TransactionJpaEntity> spec = historySpec(
                accountId, type, fromDate, toDate, minAmount, maxAmount
        );
        Pageable pageable = PageRequest.of(page, size, toSort(sortBy, sortDirection));
        return springDataRepository.findAll(spec, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByAccountFiltered(
            UUID accountId,
            TransactionType type,
            Instant fromDate,
            Instant toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount
    ) {
        return springDataRepository.count(historySpec(
                accountId, type, fromDate, toDate, minAmount, maxAmount
        ));
    }

    @Override
    public List<Transaction> findByAccountInPeriod(
            UUID accountId,
            Instant fromDate,
            Instant toDate
    ) {
        Specification<TransactionJpaEntity> spec = historySpec(
                accountId, null, fromDate, toDate, null, null
        );
        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
        return springDataRepository.findAll(spec, sort).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Money> findBalanceAfterBefore(UUID accountId, Instant before) {
        Specification<TransactionJpaEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("accountId"), accountId),
                cb.lessThan(root.get("createdAt"), before)
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        return springDataRepository.findAll(spec, pageable).getContent().stream()
                .findFirst()
                .map(entity -> Money.of(entity.getBalanceAfter()));
    }

    private static Specification<TransactionJpaEntity> historySpec(
            UUID accountId,
            TransactionType type,
            Instant fromDate,
            Instant toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("accountId"), accountId));
            if (type != null) {
                predicates.add(cb.equal(root.get("transactionType"), type.name()));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Sort toSort(String sortBy, String sortDirection) {
        if (sortBy == null || !ALLOWED_SORT.contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported sort property: " + sortBy);
        }
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, SORT_TO_ENTITY.get(sortBy));
    }

    private Transaction toDomain(TransactionJpaEntity entity) {
        return new Transaction(
                entity.getId(),
                entity.getAccountId(),
                entity.getReferenceId(),
                TransactionType.valueOf(entity.getTransactionType()),
                Money.of(entity.getAmount()),
                Money.of(entity.getBalanceAfter()),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }
}
