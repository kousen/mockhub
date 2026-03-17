CREATE TABLE categories (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) UNIQUE NOT NULL,
    slug       VARCHAR(100) UNIQUE NOT NULL,
    icon       VARCHAR(50),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

INSERT INTO categories (name, slug, icon, sort_order) VALUES
    ('Concerts', 'concerts', 'music', 1),
    ('Sports', 'sports', 'trophy', 2),
    ('Theater', 'theater', 'drama', 3),
    ('Comedy', 'comedy', 'laugh', 4),
    ('Festivals', 'festivals', 'party', 5),
    ('Other', 'other', 'star', 6);

CREATE TABLE tags (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) UNIQUE NOT NULL,
    slug       VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

INSERT INTO tags (name, slug) VALUES
    ('Rock', 'rock'),
    ('Pop', 'pop'),
    ('Hip-Hop', 'hip-hop'),
    ('Country', 'country'),
    ('Jazz', 'jazz'),
    ('Classical', 'classical'),
    ('NBA', 'nba'),
    ('NFL', 'nfl'),
    ('MLB', 'mlb'),
    ('NHL', 'nhl'),
    ('Broadway', 'broadway'),
    ('Stand-Up', 'stand-up'),
    ('Indoor', 'indoor'),
    ('Outdoor', 'outdoor');

CREATE TABLE events (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id          BIGINT NOT NULL REFERENCES venues (id),
    category_id       BIGINT NOT NULL REFERENCES categories (id),
    name              VARCHAR(255) NOT NULL,
    slug              VARCHAR(255) UNIQUE NOT NULL,
    description       TEXT,
    artist_name       VARCHAR(255),
    event_date        TIMESTAMP WITH TIME ZONE NOT NULL,
    doors_open_at     TIMESTAMP WITH TIME ZONE,
    status            VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    base_price        DECIMAL(10, 2) NOT NULL,
    min_price         DECIMAL(10, 2),
    max_price         DECIMAL(10, 2),
    total_tickets     INT NOT NULL,
    available_tickets INT NOT NULL,
    is_featured       BOOLEAN NOT NULL DEFAULT FALSE,
    search_vector     TSVECTOR,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_venue ON events (venue_id);
CREATE INDEX idx_events_category ON events (category_id);
CREATE INDEX idx_events_date ON events (event_date);
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_slug ON events (slug);
CREATE INDEX idx_events_featured ON events (is_featured) WHERE is_featured = TRUE;
CREATE INDEX idx_events_search ON events USING GIN (search_vector);

CREATE TABLE event_tags (
    event_id BIGINT NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    tag_id   BIGINT NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, tag_id)
);
