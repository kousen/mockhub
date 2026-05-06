CREATE TABLE agent_purchase_approvals (
    id BIGSERIAL PRIMARY KEY,
    approval_id VARCHAR(36) NOT NULL UNIQUE,
    user_email VARCHAR(255) NOT NULL,
    agent_id VARCHAR(255) NOT NULL,
    mandate_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL,
    proposed_order_snapshot TEXT NOT NULL,
    agent_rationale TEXT,
    subtotal NUMERIC(10, 2) NOT NULL,
    service_fee NUMERIC(10, 2) NOT NULL,
    total NUMERIC(10, 2) NOT NULL,
    commerce_policy_snapshot TEXT,
    proposed_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ,
    denied_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    final_order_number VARCHAR(20),
    denial_reason TEXT,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_agent_purchase_approvals_user_status
    ON agent_purchase_approvals(user_email, status);

CREATE INDEX idx_agent_purchase_approvals_agent_user
    ON agent_purchase_approvals(agent_id, user_email);

CREATE INDEX idx_agent_purchase_approvals_mandate
    ON agent_purchase_approvals(mandate_id);
