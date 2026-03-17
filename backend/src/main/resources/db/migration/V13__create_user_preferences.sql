CREATE TABLE user_preferences (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                 BIGINT UNIQUE NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    preferred_categories    JSONB,
    preferred_venues        JSONB,
    max_price               DECIMAL(10, 2),
    notification_settings   JSONB,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
