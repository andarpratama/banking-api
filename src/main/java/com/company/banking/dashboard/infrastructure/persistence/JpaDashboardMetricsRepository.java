package com.company.banking.dashboard.infrastructure.persistence;

import com.company.banking.dashboard.domain.DashboardMetricsRepository;
import com.company.banking.dashboard.domain.VolumeStats;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * Native aggregate queries for dashboard metrics.
 * Uses {@code idx_accounts_status}, {@code idx_transactions_created_at}
 * / {@code idx_transactions_created_at_type} — no per-row N+1.
 */
@Repository
public class JpaDashboardMetricsRepository implements DashboardMetricsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countTotalCustomers() {
        Number result = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM customers")
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long countActiveCustomers() {
        Number result = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM customers WHERE is_deleted = false")
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long countTotalAccounts() {
        Number result = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM accounts")
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long countActiveAccounts() {
        Number result = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM accounts WHERE status = 'ACTIVE'")
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public BigDecimal sumTotalBalance() {
        Object result = entityManager
                .createNativeQuery("SELECT COALESCE(SUM(balance), 0) FROM accounts")
                .getSingleResult();
        return toBigDecimal(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, VolumeStats> volumeByTypeSince(Instant fromInclusive) {
        List<Object[]> rows = entityManager
                .createNativeQuery(
                        """
                        SELECT transaction_type, COUNT(*), COALESCE(SUM(amount), 0)
                        FROM transactions
                        WHERE created_at >= :fromInclusive
                        GROUP BY transaction_type
                        """
                )
                .setParameter("fromInclusive", Timestamp.from(fromInclusive))
                .getResultList();

        Map<String, VolumeStats> byType = new HashMap<>();
        for (Object[] row : rows) {
            String type = (String) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal amount = toBigDecimal(row[2]);
            byType.put(type, new VolumeStats(count, amount));
        }
        return byType;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.setScale(2);
        }
        return new BigDecimal(value.toString()).setScale(2);
    }
}
