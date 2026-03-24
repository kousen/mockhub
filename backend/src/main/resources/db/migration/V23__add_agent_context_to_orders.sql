ALTER TABLE orders
    ADD COLUMN agent_id VARCHAR(255),
    ADD COLUMN mandate_id VARCHAR(36);

CREATE INDEX idx_orders_agent_id ON orders (agent_id);
CREATE INDEX idx_orders_mandate_id ON orders (mandate_id);
