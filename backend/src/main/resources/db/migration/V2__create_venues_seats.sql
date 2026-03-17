CREATE TABLE venues (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(255) UNIQUE NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(50) NOT NULL,
    zip_code      VARCHAR(20) NOT NULL,
    country       VARCHAR(50) NOT NULL DEFAULT 'US',
    latitude      DECIMAL(9, 6),
    longitude     DECIMAL(9, 6),
    capacity      INT NOT NULL,
    venue_type    VARCHAR(50) NOT NULL,
    image_url     VARCHAR(500),
    svg_map_url   VARCHAR(500),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_venues_slug ON venues (slug);
CREATE INDEX idx_venues_city ON venues (city);

CREATE TABLE sections (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id     BIGINT NOT NULL REFERENCES venues (id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    section_type VARCHAR(50) NOT NULL,
    capacity     INT NOT NULL,
    sort_order   INT NOT NULL DEFAULT 0,
    svg_path_id  VARCHAR(100),
    svg_x        DECIMAL(8, 2),
    svg_y        DECIMAL(8, 2),
    svg_width    DECIMAL(8, 2),
    svg_height   DECIMAL(8, 2),
    color_hex    VARCHAR(7),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (venue_id, name)
);

CREATE INDEX idx_sections_venue ON sections (venue_id);

CREATE TABLE seat_rows (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    section_id   BIGINT NOT NULL REFERENCES sections (id) ON DELETE CASCADE,
    row_label    VARCHAR(10) NOT NULL,
    seat_count   INT NOT NULL,
    sort_order   INT NOT NULL,
    svg_y_offset DECIMAL(8, 2),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (section_id, row_label)
);

CREATE TABLE seats (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    row_id      BIGINT NOT NULL REFERENCES seat_rows (id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,
    seat_type   VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    svg_x       DECIMAL(8, 2),
    svg_y       DECIMAL(8, 2),
    is_aisle    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (row_id, seat_number)
);

CREATE INDEX idx_seats_row ON seats (row_id);
