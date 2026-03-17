CREATE TABLE event_images (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id      BIGINT NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    url           VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    alt_text      VARCHAR(255),
    sort_order    INT NOT NULL DEFAULT 0,
    is_primary    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
