-- Migration to rename PayPal-specific columns to generic provider-agnostic names
-- This maintains backward compatibility while making the schema provider-neutral

-- Rename paypal_payment_id to provider_payment_id
ALTER TABLE payments RENAME COLUMN paypal_payment_id TO provider_payment_id;

-- Rename paypal_payer_id to provider_payer_id  
ALTER TABLE payments RENAME COLUMN paypal_payer_id TO provider_payer_id;

-- Rename paypal_response to provider_response
ALTER TABLE payments RENAME COLUMN paypal_response TO provider_response;

-- Update any existing indexes to use new column names
-- Note: This assumes there might be indexes on these columns
-- If indexes exist with old names, they would need to be recreated

-- Add comments to document the purpose of these generic columns
COMMENT ON COLUMN payments.provider_payment_id IS 'Payment ID from the payment provider (PayPal, Stripe, etc.)';
COMMENT ON COLUMN payments.provider_payer_id IS 'Payer ID from the payment provider';
COMMENT ON COLUMN payments.provider_response IS 'Raw response data from the payment provider'; 