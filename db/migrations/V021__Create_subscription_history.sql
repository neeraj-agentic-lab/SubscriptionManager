-- Migration: V021__Create_subscription_history.sql
-- Description: Create subscription_history table for audit trail of all subscription changes
-- Author: Neeraj Yadav
-- Date: 2026-02-09

-- Create subscription history table
CREATE TABLE subscription_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL REFERENCES tenants(id),
  subscription_id UUID NOT NULL REFERENCES subscriptions(id),
  action VARCHAR(50) NOT NULL,
  performed_by UUID NOT NULL REFERENCES users(id),
  performed_by_type VARCHAR(50) NOT NULL,
  performed_at TIMESTAMP NOT NULL DEFAULT NOW(),
  metadata JSONB,
  notes TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Add indexes for common queries
CREATE INDEX idx_subscription_history_subscription ON subscription_history(subscription_id);
CREATE INDEX idx_subscription_history_tenant ON subscription_history(tenant_id);
CREATE INDEX idx_subscription_history_performed_by ON subscription_history(performed_by);
CREATE INDEX idx_subscription_history_performed_at ON subscription_history(performed_at DESC);
CREATE INDEX idx_subscription_history_action ON subscription_history(action);

-- Add check constraint for valid actions
ALTER TABLE subscription_history ADD CONSTRAINT subscription_history_action_check 
  CHECK (action IN (
    'CREATED', 
    'PAUSED', 
    'RESUMED', 
    'CANCELED', 
    'PLAN_CHANGED', 
    'PAYMENT_UPDATED', 
    'PRODUCTS_UPDATED', 
    'SHIPPING_UPDATED', 
    'METADATA_UPDATED', 
    'ARCHIVED',
    'RESTORED',
    'BILLING_CYCLE_CHANGED',
    'PRICE_UPDATED'
  ));

-- Add check constraint for valid performer types
ALTER TABLE subscription_history ADD CONSTRAINT subscription_history_performer_type_check 
  CHECK (performed_by_type IN ('ADMIN', 'CUSTOMER', 'SYSTEM'));

-- Add comments
COMMENT ON TABLE subscription_history IS 'Audit trail for all subscription changes';
COMMENT ON COLUMN subscription_history.action IS 'Type of action performed (CREATED, PAUSED, RESUMED, CANCELED, etc.)';
COMMENT ON COLUMN subscription_history.performed_by IS 'User ID who performed the action';
COMMENT ON COLUMN subscription_history.performed_by_type IS 'Whether action was by ADMIN, CUSTOMER, or SYSTEM';
COMMENT ON COLUMN subscription_history.metadata IS 'Additional context about the change (old/new values, reasons, etc.)';
COMMENT ON COLUMN subscription_history.notes IS 'Human-readable notes about the change';

-- Add additional audit fields to subscriptions table
ALTER TABLE subscriptions ADD COLUMN admin_notes TEXT;
ALTER TABLE subscriptions ADD COLUMN archived_at TIMESTAMP NULL;
ALTER TABLE subscriptions ADD COLUMN archived_by UUID REFERENCES users(id);

COMMENT ON COLUMN subscriptions.admin_notes IS 'Admin notes for internal tracking';
COMMENT ON COLUMN subscriptions.archived_at IS 'When subscription was soft deleted/archived';
COMMENT ON COLUMN subscriptions.archived_by IS 'User who archived the subscription';

-- Create index for archived subscriptions
CREATE INDEX idx_subscriptions_archived_at ON subscriptions(archived_at) WHERE archived_at IS NOT NULL;
