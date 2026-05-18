CREATE TABLE buyer_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    preferred_artists JSONB NOT NULL DEFAULT '[]',
    preferred_categories JSONB NOT NULL DEFAULT '[]',
    preferred_cities JSONB NOT NULL DEFAULT '[]',
    preferred_venues JSONB NOT NULL DEFAULT '[]',
    preferred_sections JSONB NOT NULL DEFAULT '[]',
    disliked_venues JSONB NOT NULL DEFAULT '[]',
    disliked_categories JSONB NOT NULL DEFAULT '[]',
    max_total_price NUMERIC(12, 2),
    max_service_fee_percent NUMERIC(5, 2),
    all_in_price_only BOOLEAN NOT NULL DEFAULT FALSE,
    accessibility_needs VARCHAR(1000),
    risk_tolerance VARCHAR(30) NOT NULL DEFAULT 'BEST_VALUE',
    willing_to_wait_for_price_drops BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_buyer_preferences_user_id ON buyer_preferences(user_id);
