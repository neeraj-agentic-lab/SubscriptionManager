-- Migration: V020__Add_plan_validation_fields.sql
-- Description: Add validation fields to plans table for plan category enforcement
-- Author: Neeraj Yadav
-- Date: 2026-02-09

-- Add plan category and validation fields
ALTER TABLE plans ADD COLUMN plan_category VARCHAR(50) NOT NULL DEFAULT 'DIGITAL';
ALTER TABLE plans ADD COLUMN requires_products BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE plans ADD COLUMN allows_products BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE plans ADD COLUMN base_price_required BOOLEAN NOT NULL DEFAULT true;

-- Add comments
COMMENT ON COLUMN plans.plan_category IS 'DIGITAL, PRODUCT_BASED, or HYBRID - determines subscription behavior';
COMMENT ON COLUMN plans.requires_products IS 'Whether subscription MUST include products (true for PRODUCT_BASED)';
COMMENT ON COLUMN plans.allows_products IS 'Whether subscription CAN include products (true for PRODUCT_BASED and HYBRID)';
COMMENT ON COLUMN plans.base_price_required IS 'Whether plan must have base price > 0 (true for DIGITAL and HYBRID)';

-- Add check constraint for valid plan categories
ALTER TABLE plans ADD CONSTRAINT plans_category_check 
  CHECK (plan_category IN ('DIGITAL', 'PRODUCT_BASED', 'HYBRID'));

-- Add index for filtering by category
CREATE INDEX idx_plans_category ON plans(plan_category);

-- Update existing plans to have correct validation flags based on their current usage
-- DIGITAL plans: no products, base price required
UPDATE plans 
SET plan_category = 'DIGITAL',
    requires_products = false,
    allows_products = false,
    base_price_required = true
WHERE id NOT IN (
  SELECT DISTINCT p.id 
  FROM plans p
  INNER JOIN subscriptions s ON s.plan_id = p.id
  INNER JOIN subscription_items si ON si.subscription_id = s.id
);

-- PRODUCT_BASED plans: products required, base price optional
UPDATE plans 
SET plan_category = 'PRODUCT_BASED',
    requires_products = true,
    allows_products = true,
    base_price_required = false
WHERE id IN (
  SELECT DISTINCT p.id 
  FROM plans p
  INNER JOIN subscriptions s ON s.plan_id = p.id
  INNER JOIN subscription_items si ON si.subscription_id = s.id
)
AND (base_price_cents = 0 OR base_price_cents IS NULL);

-- HYBRID plans: products optional, base price required
UPDATE plans 
SET plan_category = 'HYBRID',
    requires_products = false,
    allows_products = true,
    base_price_required = true
WHERE id IN (
  SELECT DISTINCT p.id 
  FROM plans p
  INNER JOIN subscriptions s ON s.plan_id = p.id
  INNER JOIN subscription_items si ON si.subscription_id = s.id
)
AND base_price_cents > 0;
