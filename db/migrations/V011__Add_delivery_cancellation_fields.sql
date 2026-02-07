-- V011: Add cancellation fields to delivery_instances table
-- Supports tracking when and why deliveries are cancelled by customers

ALTER TABLE delivery_instances
    ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN cancellation_reason TEXT;

-- Index for querying cancelled deliveries
CREATE INDEX idx_delivery_instances_cancelled ON delivery_instances(tenant_id, cancelled_at) 
    WHERE cancelled_at IS NOT NULL;

-- Add comment for documentation
COMMENT ON COLUMN delivery_instances.cancelled_at IS 'Timestamp when the delivery was cancelled by the customer';
COMMENT ON COLUMN delivery_instances.cancellation_reason IS 'Reason provided by customer for cancelling the delivery';
