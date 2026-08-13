-- Query optimization (quick win): composite / partial indexes for hot read paths.
-- Forward-only. idx_transactions_account_id is kept (leftmost prefix of the new
-- composite); drop later if pg_stat_user_indexes shows it unused.

-- Ledger history / statement / opening-balance lookup:
--   WHERE account_id = ? ORDER BY created_at DESC LIMIT n
--   WHERE account_id = ? AND created_at < ? ORDER BY created_at DESC LIMIT 1
CREATE INDEX IF NOT EXISTS idx_transactions_account_date
    ON transactions (account_id, created_at DESC);

-- Admin customer list + dashboard COUNT WHERE is_deleted = false
CREATE INDEX IF NOT EXISTS idx_customers_not_deleted
    ON customers (created_at DESC)
    WHERE is_deleted = false;

-- Audit filter uses LOWER(actor); btree on actor cannot serve that predicate.
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_lower_created
    ON audit_logs (LOWER(actor), created_at DESC);
