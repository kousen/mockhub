CREATE TABLE transaction_logs (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id            BIGINT REFERENCES orders (id),
    user_id             BIGINT NOT NULL REFERENCES users (id),
    transaction_type    VARCHAR(50) NOT NULL,
    amount              DECIMAL(10, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'USD',
    provider            VARCHAR(30) NOT NULL,
    provider_reference  VARCHAR(255),
    status              VARCHAR(30) NOT NULL,
    metadata            JSONB,
    ip_address          VARCHAR(45),
    user_agent          VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_txn_logs_order ON transaction_logs (order_id);
CREATE INDEX idx_txn_logs_user ON transaction_logs (user_id);
CREATE INDEX idx_txn_logs_type ON transaction_logs (transaction_type);
