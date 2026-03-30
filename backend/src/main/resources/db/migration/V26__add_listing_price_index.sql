-- Index to support findTickets ORDER BY computed_price with status filter
-- The partial index on ACTIVE listings avoids scanning non-active rows
CREATE INDEX idx_listings_active_price ON listings (computed_price ASC)
    WHERE status = 'ACTIVE';
