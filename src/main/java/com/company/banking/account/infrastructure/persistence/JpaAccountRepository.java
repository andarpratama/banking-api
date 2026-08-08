package com.company.banking.account.infrastructure.persistence;

import com.company.banking.account.domain.Account;
import com.company.banking.account.domain.AccountRepository;
import com.company.banking.account.domain.AccountStatus;
import com.company.banking.account.domain.AccountType;
import com.company.banking.common.money.Money;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter for {@link AccountRepository}. Uses {@code @Version} for optimistic locking.
 */
@Repository
public class JpaAccountRepository implements AccountRepository {

    private final SpringDataAccountRepository springDataRepository;

    public JpaAccountRepository(SpringDataAccountRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return springDataRepository.findByAccountNumber(accountNumber).map(this::toDomain);
    }

    @Override
    public List<Account> findByCustomerId(UUID customerId) {
        return springDataRepository.findByCustomerIdOrderByCreatedAtAsc(customerId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByCustomerId(UUID customerId) {
        return springDataRepository.countByCustomerId(customerId);
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = springDataRepository.findById(account.id())
                .orElseGet(() -> new AccountJpaEntity(
                        account.id(),
                        account.accountNumber(),
                        account.customerId(),
                        account.accountType().name(),
                        account.currency(),
                        account.balance().amount(),
                        account.status().name(),
                        account.createdAt(),
                        account.updatedAt()
                ));

        if (entity.getVersion() != null) {
            entity.applyDomainState(
                    account.status().name(),
                    account.balance().amount(),
                    account.updatedAt()
            );
        }

        AccountJpaEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public int nextAccountSequence() {
        return springDataRepository.findMaxAccountSequence() + 1;
    }

    private Account toDomain(AccountJpaEntity entity) {
        long version = entity.getVersion() != null ? entity.getVersion() : 0L;
        return new Account(
                entity.getId(),
                entity.getCustomerId(),
                entity.getAccountNumber(),
                AccountType.valueOf(entity.getAccountType()),
                entity.getCurrency(),
                Money.of(entity.getBalance()),
                AccountStatus.valueOf(entity.getStatus()),
                version,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
