-- V017: Add audit fields (created_by, updated_by) to all existing tables
-- This migration adds user tracking to all tables for comprehensive audit trails

-- Tenants
ALTER TABLE tenants 
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_tenants_created_by ON tenants(created_by);
CREATE INDEX idx_tenants_updated_by ON tenants(updated_by);

COMMENT ON COLUMN tenants.created_by IS 'User who created this tenant';
COMMENT ON COLUMN tenants.updated_by IS 'User who last updated this tenant';

-- Tenant Config
ALTER TABLE tenant_config
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_tenant_config_created_by ON tenant_config(created_by);
CREATE INDEX idx_tenant_config_updated_by ON tenant_config(updated_by);

-- Customers
ALTER TABLE customers
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_customers_created_by ON customers(created_by);
CREATE INDEX idx_customers_updated_by ON customers(updated_by);

COMMENT ON COLUMN customers.created_by IS 'User who created this customer (admin or self-registered)';
COMMENT ON COLUMN customers.updated_by IS 'User who last updated this customer';

-- Plans
ALTER TABLE plans
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_plans_created_by ON plans(created_by);
CREATE INDEX idx_plans_updated_by ON plans(updated_by);

COMMENT ON COLUMN plans.created_by IS 'User who created this plan';
COMMENT ON COLUMN plans.updated_by IS 'User who last updated this plan';

-- Subscriptions
ALTER TABLE subscriptions
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_subscriptions_created_by ON subscriptions(created_by);
CREATE INDEX idx_subscriptions_updated_by ON subscriptions(updated_by);

COMMENT ON COLUMN subscriptions.created_by IS 'User who created this subscription (admin on behalf of customer, or customer self-service)';
COMMENT ON COLUMN subscriptions.updated_by IS 'User who last updated this subscription';

-- Subscription Items
ALTER TABLE subscription_items
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_subscription_items_created_by ON subscription_items(created_by);
CREATE INDEX idx_subscription_items_updated_by ON subscription_items(updated_by);

-- Invoices
ALTER TABLE invoices
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_invoices_created_by ON invoices(created_by);
CREATE INDEX idx_invoices_updated_by ON invoices(updated_by);

-- Invoice Lines
ALTER TABLE invoice_lines
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_invoice_lines_created_by ON invoice_lines(created_by);
CREATE INDEX idx_invoice_lines_updated_by ON invoice_lines(updated_by);

-- Payment Attempts
ALTER TABLE payment_attempts
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_payment_attempts_created_by ON payment_attempts(created_by);
CREATE INDEX idx_payment_attempts_updated_by ON payment_attempts(updated_by);

-- Delivery Instances
ALTER TABLE delivery_instances
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_delivery_instances_created_by ON delivery_instances(created_by);
CREATE INDEX idx_delivery_instances_updated_by ON delivery_instances(updated_by);

COMMENT ON COLUMN delivery_instances.created_by IS 'User who created this delivery (usually system)';
COMMENT ON COLUMN delivery_instances.updated_by IS 'User who last updated this delivery (skip, reschedule, fulfill)';

-- Entitlements
ALTER TABLE entitlements
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_entitlements_created_by ON entitlements(created_by);
CREATE INDEX idx_entitlements_updated_by ON entitlements(updated_by);

-- Webhook Endpoints
ALTER TABLE webhook_endpoints
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_webhook_endpoints_created_by ON webhook_endpoints(created_by);
CREATE INDEX idx_webhook_endpoints_updated_by ON webhook_endpoints(updated_by);

COMMENT ON COLUMN webhook_endpoints.created_by IS 'User who registered this webhook endpoint';
COMMENT ON COLUMN webhook_endpoints.updated_by IS 'User who last updated this webhook endpoint';

-- Webhook Deliveries (system-generated, but track who might have triggered retry)
ALTER TABLE webhook_deliveries
  ADD COLUMN created_by UUID REFERENCES users(id),
  ADD COLUMN updated_by UUID REFERENCES users(id);

CREATE INDEX idx_webhook_deliveries_created_by ON webhook_deliveries(created_by);
CREATE INDEX idx_webhook_deliveries_updated_by ON webhook_deliveries(updated_by);

-- Job Configuration
ALTER TABLE job_configuration
  ADD COLUMN created_by_user_id UUID REFERENCES users(id),
  ADD COLUMN updated_by_user_id UUID REFERENCES users(id);

CREATE INDEX idx_job_configuration_created_by ON job_configuration(created_by_user_id);
CREATE INDEX idx_job_configuration_updated_by ON job_configuration(updated_by_user_id);

COMMENT ON COLUMN job_configuration.created_by_user_id IS 'User who created this job configuration';
COMMENT ON COLUMN job_configuration.updated_by_user_id IS 'User who last updated this job configuration';

-- Note: job_configuration already has created_by/updated_by as VARCHAR
-- We're adding user_id foreign keys with different names to maintain backward compatibility
