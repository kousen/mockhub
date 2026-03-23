CREATE TABLE mandates (
    id BIGSERIAL PRIMARY KEY,
    mandate_id VARCHAR(36) NOT NULL UNIQUE,
    agent_id VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    max_spend_per_transaction NUMERIC(12,2),
    max_spend_total NUMERIC(12,2),
    total_spent NUMERIC(12,2) NOT NULL DEFAULT 0,
    allowed_categories VARCHAR(1000),
    allowed_events VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_mandates_agent_user_status ON mandates(agent_id, user_email, status);
CREATE INDEX idx_mandates_mandate_id ON mandates(mandate_id);
