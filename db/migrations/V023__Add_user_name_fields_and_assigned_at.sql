-- Migration: V023__Add_user_name_fields_and_assigned_at.sql
-- Description: Add first_name and last_name to users table, add assigned_at to user_tenants
-- Author: Neeraj Yadav
-- Date: 2026-02-10

-- Add first_name and last_name to users table
ALTER TABLE users ADD COLUMN first_name VARCHAR(100);
ALTER TABLE users ADD COLUMN last_name VARCHAR(100);

-- Migrate existing full_name data to first_name and last_name
UPDATE users 
SET first_name = SPLIT_PART(full_name, ' ', 1),
    last_name = CASE 
        WHEN full_name LIKE '% %' THEN SUBSTRING(full_name FROM POSITION(' ' IN full_name) + 1)
        ELSE ''
    END;

-- Make first_name and last_name NOT NULL after migration
ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN last_name SET NOT NULL;

-- Add comments
COMMENT ON COLUMN users.first_name IS 'User first name';
COMMENT ON COLUMN users.last_name IS 'User last name';

-- Add assigned_at to user_tenants table
ALTER TABLE user_tenants ADD COLUMN assigned_at TIMESTAMP NOT NULL DEFAULT NOW();

COMMENT ON COLUMN user_tenants.assigned_at IS 'When the user was assigned to this tenant';

-- Create index for assigned_at
CREATE INDEX idx_user_tenants_assigned_at ON user_tenants(assigned_at);
