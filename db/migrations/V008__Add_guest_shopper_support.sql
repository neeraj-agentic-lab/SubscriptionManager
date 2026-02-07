-- V008: Add guest shopper support
-- Allow customers without email addresses for guest checkout

-- Make email nullable for guest shoppers
ALTER TABLE customers ALTER COLUMN email DROP NOT NULL;

-- Add guest_id for anonymous identification
ALTER TABLE customers ADD COLUMN guest_id VARCHAR(255);

-- Add customer_type to distinguish registered vs guest users
ALTER TABLE customers ADD COLUMN customer_type VARCHAR(20) NOT NULL DEFAULT 'REGISTERED';

-- Add constraint for customer_type
ALTER TABLE customers ADD CONSTRAINT customers_type_check 
    CHECK (customer_type IN ('REGISTERED', 'GUEST'));

-- Drop the old unique constraint on (tenant_id, email)
ALTER TABLE customers DROP CONSTRAINT customers_tenant_email_unique;

-- Create new conditional unique constraints
-- For registered customers: email must be unique and not null
CREATE UNIQUE INDEX idx_customers_registered_email 
    ON customers(tenant_id, email) 
    WHERE customer_type = 'REGISTERED' AND email IS NOT NULL;

-- For guest customers: guest_id must be unique when provided
CREATE UNIQUE INDEX idx_customers_guest_id 
    ON customers(tenant_id, guest_id) 
    WHERE customer_type = 'GUEST' AND guest_id IS NOT NULL;

-- Ensure registered customers have email
ALTER TABLE customers ADD CONSTRAINT customers_registered_email_required
    CHECK (
        (customer_type = 'REGISTERED' AND email IS NOT NULL) OR 
        (customer_type = 'GUEST')
    );

-- Add indexes for guest shoppers
CREATE INDEX idx_customers_guest_type ON customers(tenant_id, customer_type);
CREATE INDEX idx_customers_guest_id_lookup ON customers(guest_id) WHERE guest_id IS NOT NULL;

-- Add comment explaining guest_id usage
COMMENT ON COLUMN customers.guest_id IS 'Anonymous identifier for guest shoppers (session ID, device fingerprint, etc.)';
COMMENT ON COLUMN customers.customer_type IS 'REGISTERED for normal users, GUEST for anonymous shoppers';
