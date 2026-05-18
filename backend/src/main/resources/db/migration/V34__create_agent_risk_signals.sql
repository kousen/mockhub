CREATE TABLE agent_risk_signals (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    agent_id VARCHAR(255) NOT NULL,
    signal_type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    action_type VARCHAR(50),
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    order_number VARCHAR(30),
    mandate_id VARCHAR(100),
    amount NUMERIC(12, 2),
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_risk_signals_user_agent_created
    ON agent_risk_signals(user_email, agent_id, created_at DESC);

CREATE INDEX idx_agent_risk_signals_type_created
    ON agent_risk_signals(signal_type, created_at DESC);
