-- V022: Create API client management tables for multi-tier authentication
-- Supports API Key + HMAC, OAuth 2.0, and mTLS authentication methods

-- API Clients table
CREATE TABLE api_clients (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  
  -- Client identification
  client_id VARCHAR(255) NOT NULL UNIQUE,
  client_name VARCHAR(255) NOT NULL,
  client_type VARCHAR(50) NOT NULL, -- 'SERVER', 'SPA', 'MOBILE', 'NATIVE'
  
  -- Authentication method
  auth_method VARCHAR(50) NOT NULL, -- 'API_KEY', 'OAUTH', 'MTLS'
  client_secret_hash VARCHAR(255), -- bcrypt hash, for API_KEY and OAUTH
  
  -- mTLS specific fields
  certificate_subject VARCHAR(500),
  certificate_fingerprint VARCHAR(255),
  certificate_expires_at TIMESTAMP,
  
  -- OAuth specific fields
  redirect_uris TEXT[], -- Array of allowed redirect URIs
  
  -- Scopes and permissions
  allowed_scopes TEXT[] NOT NULL DEFAULT '{}', -- e.g., ['subscriptions:*', 'deliveries:read']
  
  -- Security settings
  allowed_ips INET[], -- IP whitelist, null = allow all
  rate_limit_per_hour INTEGER DEFAULT 1000,
  
  -- Status
  status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'SUSPENDED', 'REVOKED', 'PENDING_CERTIFICATE'
  
  -- Usage tracking
  last_used_at TIMESTAMP,
  total_requests BIGINT DEFAULT 0,
  
  -- Metadata
  description TEXT,
  metadata JSONB,
  
  -- Audit fields
  created_by UUID REFERENCES users(id),
  updated_by UUID REFERENCES users(id),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  
  CONSTRAINT api_clients_type_check CHECK (client_type IN ('SERVER', 'SPA', 'MOBILE', 'NATIVE')),
  CONSTRAINT api_clients_auth_check CHECK (auth_method IN ('API_KEY', 'OAUTH', 'MTLS')),
  CONSTRAINT api_clients_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED', 'PENDING_CERTIFICATE'))
);

CREATE INDEX idx_api_clients_tenant ON api_clients(tenant_id);
CREATE INDEX idx_api_clients_client_id ON api_clients(client_id);
CREATE INDEX idx_api_clients_status ON api_clients(status);
CREATE INDEX idx_api_clients_auth_method ON api_clients(auth_method);
CREATE INDEX idx_api_clients_created_at ON api_clients(created_at);

COMMENT ON TABLE api_clients IS 'API clients for multi-tier authentication (API Key, OAuth, mTLS)';
COMMENT ON COLUMN api_clients.client_id IS 'Public client identifier (e.g., acme_coffee_shopify_abc123)';
COMMENT ON COLUMN api_clients.client_secret_hash IS 'bcrypt hash of client secret, only for API_KEY and OAUTH';
COMMENT ON COLUMN api_clients.allowed_scopes IS 'Array of allowed scopes (e.g., subscriptions:*, deliveries:read)';
COMMENT ON COLUMN api_clients.rate_limit_per_hour IS 'Maximum requests per hour, null = unlimited';

-- API Client Scopes (granular permissions)
CREATE TABLE api_client_scopes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  scope VARCHAR(100) NOT NULL UNIQUE,
  description TEXT,
  resource_type VARCHAR(50) NOT NULL, -- 'SUBSCRIPTION', 'DELIVERY', 'CUSTOMER', 'PLAN', etc.
  permission VARCHAR(50) NOT NULL, -- 'READ', 'WRITE', 'DELETE', 'ALL'
  
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  
  CONSTRAINT api_client_scopes_permission_check CHECK (permission IN ('READ', 'WRITE', 'DELETE', 'ALL'))
);

CREATE INDEX idx_api_client_scopes_resource ON api_client_scopes(resource_type);

COMMENT ON TABLE api_client_scopes IS 'Available scopes for API clients with granular permissions';

-- Insert default scopes
INSERT INTO api_client_scopes (scope, description, resource_type, permission) VALUES
  ('subscriptions:read', 'Read subscription data', 'SUBSCRIPTION', 'READ'),
  ('subscriptions:write', 'Create and update subscriptions', 'SUBSCRIPTION', 'WRITE'),
  ('subscriptions:delete', 'Delete subscriptions', 'SUBSCRIPTION', 'DELETE'),
  ('subscriptions:*', 'Full subscription access', 'SUBSCRIPTION', 'ALL'),
  ('deliveries:read', 'Read delivery data', 'DELIVERY', 'READ'),
  ('deliveries:write', 'Update deliveries (skip, reschedule, fulfill)', 'DELIVERY', 'WRITE'),
  ('deliveries:*', 'Full delivery access', 'DELIVERY', 'ALL'),
  ('customers:read', 'Read customer data', 'CUSTOMER', 'READ'),
  ('customers:write', 'Create and update customers', 'CUSTOMER', 'WRITE'),
  ('customers:*', 'Full customer access', 'CUSTOMER', 'ALL'),
  ('plans:read', 'Read plan data', 'PLAN', 'READ'),
  ('plans:write', 'Create and update plans', 'PLAN', 'WRITE'),
  ('plans:*', 'Full plan access', 'PLAN', 'ALL'),
  ('webhooks:read', 'Read webhook configurations', 'WEBHOOK', 'READ'),
  ('webhooks:write', 'Manage webhook configurations', 'WEBHOOK', 'WRITE'),
  ('webhooks:*', 'Full webhook access', 'WEBHOOK', 'ALL'),
  ('*:*', 'Full API access (super admin only)', 'ALL', 'ALL');

-- OAuth Access Tokens (for OAuth 2.0 flow)
CREATE TABLE oauth_access_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id UUID NOT NULL REFERENCES api_clients(id) ON DELETE CASCADE,
  
  -- Token data
  access_token VARCHAR(500) NOT NULL UNIQUE,
  refresh_token VARCHAR(500) UNIQUE,
  token_type VARCHAR(50) NOT NULL DEFAULT 'Bearer',
  
  -- Scopes
  scopes TEXT[] NOT NULL,
  
  -- Expiration
  expires_at TIMESTAMP NOT NULL,
  refresh_expires_at TIMESTAMP,
  
  -- OAuth specific
  authorization_code VARCHAR(500),
  code_challenge VARCHAR(500), -- PKCE
  code_challenge_method VARCHAR(10), -- 'S256' or 'plain'
  
  -- Status
  revoked BOOLEAN DEFAULT FALSE,
  revoked_at TIMESTAMP,
  
  -- Metadata
  ip_address INET,
  user_agent TEXT,
  
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  last_used_at TIMESTAMP
);

CREATE INDEX idx_oauth_tokens_client ON oauth_access_tokens(client_id);
CREATE INDEX idx_oauth_tokens_access ON oauth_access_tokens(access_token);
CREATE INDEX idx_oauth_tokens_refresh ON oauth_access_tokens(refresh_token);
CREATE INDEX idx_oauth_tokens_expires ON oauth_access_tokens(expires_at);
CREATE INDEX idx_oauth_tokens_revoked ON oauth_access_tokens(revoked) WHERE revoked = FALSE;

COMMENT ON TABLE oauth_access_tokens IS 'OAuth 2.0 access and refresh tokens with PKCE support';

-- API Usage Logs (for rate limiting and analytics)
CREATE TABLE api_usage_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id UUID NOT NULL REFERENCES api_clients(id) ON DELETE CASCADE,
  tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  
  -- Request details
  endpoint VARCHAR(255) NOT NULL,
  http_method VARCHAR(10) NOT NULL,
  status_code INTEGER NOT NULL,
  response_time_ms INTEGER,
  
  -- Request metadata
  ip_address INET,
  user_agent TEXT,
  
  -- Error tracking
  error_message TEXT,
  
  -- Timestamp
  timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Partition by month for better performance
CREATE INDEX idx_api_usage_client ON api_usage_logs(client_id, timestamp DESC);
CREATE INDEX idx_api_usage_tenant ON api_usage_logs(tenant_id, timestamp DESC);
CREATE INDEX idx_api_usage_endpoint ON api_usage_logs(endpoint);
CREATE INDEX idx_api_usage_timestamp ON api_usage_logs(timestamp DESC);

COMMENT ON TABLE api_usage_logs IS 'API request logs for rate limiting, analytics, and debugging';

-- Request Nonces (PostgreSQL-backed replay attack prevention)
CREATE TABLE request_nonces (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id VARCHAR(255) NOT NULL,
  nonce VARCHAR(255) NOT NULL,
  timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMP NOT NULL,
  
  UNIQUE(client_id, nonce)
);

CREATE INDEX idx_request_nonces_client ON request_nonces(client_id);
CREATE INDEX idx_request_nonces_expires ON request_nonces(expires_at);

COMMENT ON TABLE request_nonces IS 'Nonce cache for replay attack prevention (10 minute TTL)';

-- Rate Limit Buckets (PostgreSQL-backed rate limiting)
CREATE TABLE rate_limit_buckets (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  client_id VARCHAR(255) NOT NULL,
  window_start TIMESTAMP NOT NULL,
  request_count INTEGER NOT NULL DEFAULT 0,
  
  UNIQUE(client_id, window_start)
);

CREATE INDEX idx_rate_limit_client ON rate_limit_buckets(client_id);
CREATE INDEX idx_rate_limit_window ON rate_limit_buckets(window_start);

COMMENT ON TABLE rate_limit_buckets IS 'Rate limiting buckets using sliding window algorithm';

-- Cleanup function for expired nonces (run every 10 minutes)
CREATE OR REPLACE FUNCTION cleanup_expired_nonces()
RETURNS void AS $$
BEGIN
  DELETE FROM request_nonces WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql;

-- Cleanup function for old rate limit buckets (run daily)
CREATE OR REPLACE FUNCTION cleanup_old_rate_limits()
RETURNS void AS $$
BEGIN
  DELETE FROM rate_limit_buckets WHERE window_start < NOW() - INTERVAL '24 hours';
END;
$$ LANGUAGE plpgsql;

-- Cleanup function for old API usage logs (run weekly, keep 90 days)
CREATE OR REPLACE FUNCTION cleanup_old_api_logs()
RETURNS void AS $$
BEGIN
  DELETE FROM api_usage_logs WHERE timestamp < NOW() - INTERVAL '90 days';
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_nonces IS 'Removes expired nonces (older than 10 minutes)';
COMMENT ON FUNCTION cleanup_old_rate_limits IS 'Removes old rate limit buckets (older than 24 hours)';
COMMENT ON FUNCTION cleanup_old_api_logs IS 'Removes old API usage logs (older than 90 days)';
