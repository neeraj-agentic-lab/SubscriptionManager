-- V999: Bootstrap super admin user for initial system setup
-- This migration creates the first super admin user with default credentials
-- Password MUST be changed on first login

-- Create bootstrap super admin
-- Default password: 'ChangeMe123!' (bcrypt hash below)
-- This user can create tenants, other users, and configure the system
INSERT INTO users (
  id,
  email,
  full_name,
  password_hash,
  role,
  status,
  must_change_password,
  created_at,
  updated_at
)
VALUES (
  '00000000-0000-0000-0000-000000000001'::UUID,
  'admin@subscriptionengine.com',
  'System Administrator',
  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYVvMpYq0Iu', -- bcrypt('ChangeMe123!')
  'SUPER_ADMIN',
  'ACTIVE',
  TRUE, -- Force password change on first login
  NOW(),
  NOW()
)
ON CONFLICT (email) DO NOTHING; -- Prevent duplicate if migration re-runs

-- Log the bootstrap operation
INSERT INTO sensitive_operations_log (
  user_id,
  operation,
  resource_type,
  resource_id,
  details,
  timestamp
)
VALUES (
  '00000000-0000-0000-0000-000000000001'::UUID,
  'BOOTSTRAP_SUPER_ADMIN',
  'USER',
  '00000000-0000-0000-0000-000000000001'::UUID,
  jsonb_build_object(
    'email', 'admin@subscriptionengine.com',
    'note', 'Initial system setup - bootstrap super admin created',
    'default_password', 'ChangeMe123!',
    'must_change_password', true
  ),
  NOW()
)
ON CONFLICT DO NOTHING;

-- Add helpful comment
COMMENT ON TABLE users IS 'Bootstrap super admin created: admin@subscriptionengine.com / ChangeMe123! (MUST CHANGE ON FIRST LOGIN)';

-- Display warning in logs
DO $$
BEGIN
  RAISE NOTICE '========================================';
  RAISE NOTICE 'BOOTSTRAP SUPER ADMIN CREATED';
  RAISE NOTICE '========================================';
  RAISE NOTICE 'Email: admin@subscriptionengine.com';
  RAISE NOTICE 'Password: ChangeMe123!';
  RAISE NOTICE 'MUST CHANGE PASSWORD ON FIRST LOGIN';
  RAISE NOTICE '========================================';
END $$;
