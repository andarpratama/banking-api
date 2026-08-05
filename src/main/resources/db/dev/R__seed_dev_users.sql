-- Dev-only seed users (loaded via spring.flyway.locations on profile=dev only).
-- Password (documented test only): SecurePass123!
-- BCrypt cost 12 hash of that password — never use these credentials in prod.
-- Idempotent: ON CONFLICT / NOT EXISTS so repeatable migration is safe.

INSERT INTO users (email, password_hash, enabled)
VALUES
    ('admin@banking.local', '$2b$12$.E07ayzWXLbELRTrdHC8e.B8Xfm0mj2YVoE4D653/BTT6SCFWr3zm', true),
    ('customer@banking.local', '$2b$12$.E07ayzWXLbELRTrdHC8e.B8Xfm0mj2YVoE4D653/BTT6SCFWr3zm', true)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.email = 'admin@banking.local'
  AND r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.email = 'customer@banking.local'
  AND r.name = 'CUSTOMER'
ON CONFLICT DO NOTHING;

INSERT INTO customers (user_id, customer_number, full_name, phone, status, is_deleted)
SELECT u.id,
       'CUST-000001',
       'Dev Seed Customer',
       '+10000000000',
       'ACTIVE',
       false
FROM users u
WHERE u.email = 'customer@banking.local'
  AND NOT EXISTS (
      SELECT 1 FROM customers c WHERE c.user_id = u.id
  );
