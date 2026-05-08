-- V024: Add custom_attrs to core business objects for extensibility
-- Date: 2026-03-08
-- Purpose: Enable tenant-specific customizations and extensions across all business objects

-- Add custom_attrs to tenants table
-- Use case: Custom branding, settings, feature flags, integration configs
ALTER TABLE tenants 
ADD COLUMN custom_attrs JSONB NOT NULL DEFAULT '{}';

COMMENT ON COLUMN tenants.custom_attrs IS 'Tenant-specific custom attributes for branding, settings, and feature flags';

-- Add custom_attrs to users table
-- Use case: Custom preferences, profile data, notification settings, avatar URL
ALTER TABLE users 
ADD COLUMN custom_attrs JSONB NOT NULL DEFAULT '{}';

COMMENT ON COLUMN users.custom_attrs IS 'User-specific custom attributes for preferences, profile data, and settings';

-- Add custom_attrs to user_tenants table
-- Use case: Role-specific metadata, department, cost center, access overrides
ALTER TABLE user_tenants 
ADD COLUMN custom_attrs JSONB NOT NULL DEFAULT '{}';

COMMENT ON COLUMN user_tenants.custom_attrs IS 'User-tenant relationship metadata for department, cost center, and role-specific settings';

-- Add custom_attrs to subscription_history table
-- Note: This is separate from the existing 'metadata' column
-- metadata = audit context (what changed, old/new values)
-- custom_attrs = tenant-specific extensibility (tags, categories, custom fields)
ALTER TABLE subscription_history 
ADD COLUMN custom_attrs JSONB NOT NULL DEFAULT '{}';

COMMENT ON COLUMN subscription_history.custom_attrs IS 'Custom attributes for tenant-specific categorization and tagging of history events';
COMMENT ON COLUMN subscription_history.metadata IS 'Audit trail context: what changed, old/new values, reasons (system-managed)';

-- Add custom_attrs to webhook_deliveries table
-- Use case: Custom retry logic, debugging info, correlation IDs, delivery metadata
ALTER TABLE webhook_deliveries 
ADD COLUMN custom_attrs JSONB NOT NULL DEFAULT '{}';

COMMENT ON COLUMN webhook_deliveries.custom_attrs IS 'Custom attributes for retry logic, debugging info, correlation IDs, and delivery tracking';

-- Add custom_attrs to admin_sessions table
-- Use case: Device fingerprinting, session metadata, security flags, location data
ALTER TABLE admin_sessions 
ADD COLUMN custom_attrs JSONB NOT NULL DEFAULT '{}';

COMMENT ON COLUMN admin_sessions.custom_attrs IS 'Custom attributes for device fingerprinting, session metadata, and security flags';

-- Rename job_execution_history.metadata to custom_attrs for consistency
-- This column is not currently used in code, safe to rename
ALTER TABLE job_execution_history 
RENAME COLUMN metadata TO custom_attrs;

-- Ensure it has proper constraints
ALTER TABLE job_execution_history 
ALTER COLUMN custom_attrs SET NOT NULL,
ALTER COLUMN custom_attrs SET DEFAULT '{}';

COMMENT ON COLUMN job_execution_history.custom_attrs IS 'Custom attributes for job execution context, debugging info, and statistics';

-- Create GIN indexes for efficient JSONB queries on new columns
CREATE INDEX idx_tenants_custom_attrs ON tenants USING GIN (custom_attrs);
CREATE INDEX idx_users_custom_attrs ON users USING GIN (custom_attrs);
CREATE INDEX idx_user_tenants_custom_attrs ON user_tenants USING GIN (custom_attrs);
CREATE INDEX idx_subscription_history_custom_attrs ON subscription_history USING GIN (custom_attrs);
CREATE INDEX idx_webhook_deliveries_custom_attrs ON webhook_deliveries USING GIN (custom_attrs);
CREATE INDEX idx_admin_sessions_custom_attrs ON admin_sessions USING GIN (custom_attrs);
CREATE INDEX idx_job_execution_history_custom_attrs ON job_execution_history USING GIN (custom_attrs);

-- Summary of changes:
-- 1. tenants.custom_attrs - NEW column for tenant extensibility
-- 2. users.custom_attrs - NEW column for user extensibility
-- 3. user_tenants.custom_attrs - NEW column for relationship metadata
-- 4. subscription_history.custom_attrs - NEW column (metadata column kept for audit context)
-- 5. webhook_deliveries.custom_attrs - NEW column for retry logic and correlation IDs
-- 6. admin_sessions.custom_attrs - NEW column for device fingerprinting and security flags
-- 7. job_execution_history.metadata → custom_attrs - RENAMED for consistency
-- 8. GIN indexes added for efficient JSONB queries on all custom_attrs columns
