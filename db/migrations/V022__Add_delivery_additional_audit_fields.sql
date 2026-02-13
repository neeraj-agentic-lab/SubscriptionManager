-- Migration: V022__Add_delivery_additional_audit_fields.sql
-- Description: Add additional audit fields to delivery_instances table
-- Author: Neeraj Yadav
-- Date: 2026-02-09

-- Add rescheduling audit fields
ALTER TABLE delivery_instances ADD COLUMN rescheduled_by UUID REFERENCES users(id);
ALTER TABLE delivery_instances ADD COLUMN reschedule_reason TEXT;
ALTER TABLE delivery_instances ADD COLUMN rescheduled_at TIMESTAMP NULL;

-- Add comments
COMMENT ON COLUMN delivery_instances.rescheduled_by IS 'User who rescheduled the delivery';
COMMENT ON COLUMN delivery_instances.reschedule_reason IS 'Reason for rescheduling the delivery';
COMMENT ON COLUMN delivery_instances.rescheduled_at IS 'When the delivery was rescheduled';

-- Create index for rescheduled deliveries
CREATE INDEX idx_delivery_instances_rescheduled ON delivery_instances(rescheduled_at) WHERE rescheduled_at IS NOT NULL;
