CREATE TABLE tickets (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id    BIGINT NOT NULL REFERENCES events (id),
    seat_id     BIGINT REFERENCES seats (id),
    section_id  BIGINT NOT NULL REFERENCES sections (id),
    ticket_type VARCHAR(30) NOT NULL,
    face_value  DECIMAL(10, 2) NOT NULL,
    status      VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    barcode     VARCHAR(100) UNIQUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tickets_event ON tickets (event_id);
CREATE INDEX idx_tickets_section ON tickets (section_id);
CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_event_status ON tickets (event_id, status);

CREATE TABLE listings (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id        BIGINT NOT NULL REFERENCES tickets (id),
    event_id         BIGINT NOT NULL REFERENCES events (id),
    listed_price     DECIMAL(10, 2) NOT NULL,
    computed_price   DECIMAL(10, 2) NOT NULL,
    price_multiplier DECIMAL(5, 3) NOT NULL DEFAULT 1.000,
    status           VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    listed_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at       TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listings_event ON listings (event_id);
CREATE INDEX idx_listings_ticket ON listings (ticket_id);
CREATE INDEX idx_listings_status ON listings (status);
CREATE INDEX idx_listings_event_active ON listings (event_id, status) WHERE status = 'ACTIVE';
