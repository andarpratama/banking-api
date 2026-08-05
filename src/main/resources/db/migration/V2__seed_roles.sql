-- Reference roles required in every environment (RBAC).
-- Idempotent: safe if re-applied or if rows already exist.
INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Administrator with full system access'),
    ('CUSTOMER', 'Regular customer with limited access')
ON CONFLICT (name) DO NOTHING;
