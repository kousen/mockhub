CREATE TABLE price_history (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id     BIGINT NOT NULL REFERENCES events (id),
    listing_id   BIGINT REFERENCES listings (id),
    price        DECIMAL(10, 2) NOT NULL,
    multiplier   DECIMAL(5, 3) NOT NULL,
    supply_ratio DECIMAL(5, 4) NOT NULL,
    demand_score DECIMAL(5, 4),
    days_to_event INT NOT NULL,
    recorded_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_price_history_event ON price_history (event_id);
CREATE INDEX idx_price_history_recorded ON price_history (recorded_at);
CREATE INDEX idx_price_history_event_time ON price_history (event_id, recorded_at);
