-- T-051: support efficient dashboard volume aggregates by time + type
CREATE INDEX IF NOT EXISTS idx_transactions_created_at_type
    ON transactions (created_at, transaction_type);
