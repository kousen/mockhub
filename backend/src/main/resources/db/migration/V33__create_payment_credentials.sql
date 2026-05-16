CREATE TABLE payment_credentials (
    id BIGSERIAL PRIMARY KEY,
    credential_id VARCHAR(36) NOT NULL UNIQUE,
    user_email VARCHAR(255) NOT NULL,
    agent_id VARCHAR(255) NOT NULL,
    allowed_merchant VARCHAR(100) NOT NULL,
    max_amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    usage_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    backing_payment_method VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    consumed_at TIMESTAMPTZ,
    consumed_by_order_number VARCHAR(20),
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_credentials_user_status
    ON payment_credentials(user_email, status);

CREATE INDEX idx_payment_credentials_agent_user
    ON payment_credentials(agent_id, user_email);

CREATE INDEX idx_payment_credentials_status_expiry
    ON payment_credentials(status, expires_at);
