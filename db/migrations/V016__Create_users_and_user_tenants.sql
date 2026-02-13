-- V016: Create users and user_tenants tables for user management system
-- This migration adds support for admin, staff, and customer users with multi-tenant access

-- Users table for all system users
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) NOT NULL UNIQUE,
  full_name VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255), -- bcrypt hash, nullable for OAuth-only users
  role VARCHAR(50) NOT NULL, -- 'SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF', 'CUSTOMER'
  status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'SUSPENDED', 'DELETED'
  
  -- Security fields
  mfa_enabled BOOLEAN DEFAULT FALSE,
  mfa_secret VARCHAR(255),
  must_change_password BOOLEAN DEFAULT FALSE,
  last_login_at TIMESTAMP,
  failed_login_attempts INTEGER DEFAULT 0,
  locked_until TIMESTAMP,
  
  -- Metadata
  metadata JSONB,
  
  -- Timestamps
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  
  CONSTRAINT users_role_check CHECK (role IN ('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF', 'CUSTOMER')),
  CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at);

COMMENT ON TABLE users IS 'System users including admins, staff, and customers';
COMMENT ON COLUMN users.role IS 'SUPER_ADMIN: platform admin, TENANT_ADMIN: tenant owner, STAFF: tenant employee, CUSTOMER: end customer';
COMMENT ON COLUMN users.password_hash IS 'bcrypt hash of password, nullable for OAuth-only users';
COMMENT ON COLUMN users.must_change_password IS 'Force password change on next login (e.g., for bootstrap admin)';

-- User-Tenant relationships (multi-tenant access)
CREATE TABLE user_tenants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  role VARCHAR(50) NOT NULL, -- 'OWNER', 'ADMIN', 'MEMBER', 'VIEWER'
  
  -- Timestamps
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  
  UNIQUE(user_id, tenant_id),
  CONSTRAINT user_tenants_role_check CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);

CREATE INDEX idx_user_tenants_user ON user_tenants(user_id);
CREATE INDEX idx_user_tenants_tenant ON user_tenants(tenant_id);
CREATE INDEX idx_user_tenants_role ON user_tenants(role);

COMMENT ON TABLE user_tenants IS 'Maps users to tenants with specific roles for multi-tenant access';
COMMENT ON COLUMN user_tenants.role IS 'OWNER: full control, ADMIN: manage users/settings, MEMBER: standard access, VIEWER: read-only';

-- Admin sessions for JWT token tracking
CREATE TABLE admin_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  revoked BOOLEAN DEFAULT FALSE,
  revoked_at TIMESTAMP,
  
  -- Request metadata
  ip_address INET,
  user_agent TEXT,
  
  -- Timestamps
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  last_used_at TIMESTAMP
);

CREATE INDEX idx_admin_sessions_user ON admin_sessions(user_id);
CREATE INDEX idx_admin_sessions_token ON admin_sessions(token_hash);
CREATE INDEX idx_admin_sessions_expires ON admin_sessions(expires_at);
CREATE INDEX idx_admin_sessions_revoked ON admin_sessions(revoked) WHERE revoked = FALSE;

COMMENT ON TABLE admin_sessions IS 'Tracks active admin/staff JWT sessions for revocation and auditing';

-- Sensitive operations audit log
CREATE TABLE sensitive_operations_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id),
  operation VARCHAR(100) NOT NULL, -- 'CREATE_API_CLIENT', 'ROTATE_SECRET', 'DELETE_USER', etc.
  resource_type VARCHAR(50) NOT NULL, -- 'USER', 'API_CLIENT', 'TENANT', etc.
  resource_id UUID,
  tenant_id UUID REFERENCES tenants(id),
  
  -- Request metadata
  ip_address INET,
  user_agent TEXT,
  mfa_verified BOOLEAN DEFAULT FALSE,
  
  -- Details
  details JSONB,
  
  -- Timestamp
  timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sensitive_ops_user ON sensitive_operations_log(user_id);
CREATE INDEX idx_sensitive_ops_tenant ON sensitive_operations_log(tenant_id);
CREATE INDEX idx_sensitive_ops_operation ON sensitive_operations_log(operation);
CREATE INDEX idx_sensitive_ops_time ON sensitive_operations_log(timestamp DESC);
CREATE INDEX idx_sensitive_ops_resource ON sensitive_operations_log(resource_type, resource_id);

COMMENT ON TABLE sensitive_operations_log IS 'Audit log for sensitive operations like user management, API client creation, etc.';
