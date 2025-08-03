-- Add raw request field to payments table for debugging
ALTER TABLE payments ADD COLUMN raw_request TEXT;

-- Create payment_audits table for comprehensive audit trail
CREATE TABLE payment_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    request_url VARCHAR(500),
    request_method VARCHAR(10),
    request_headers TEXT,
    request_body TEXT,
    response_status INTEGER,
    response_headers TEXT,
    response_body TEXT,
    duration_ms BIGINT,
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (payment_id) REFERENCES payments(id),
    INDEX idx_payment_audit_payment_id (payment_id),
    INDEX idx_payment_audit_action (action),
    INDEX idx_payment_audit_created_at (created_at),
    INDEX idx_payment_audit_response_status (response_status)
); 