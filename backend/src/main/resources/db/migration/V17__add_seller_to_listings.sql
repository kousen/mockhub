-- Add seller identity to listings for the seller flow.
-- NULL seller_id = platform/primary-market listing (seeded inventory).
-- Non-null seller_id = user-created resale listing.
ALTER TABLE listings ADD COLUMN seller_id BIGINT REFERENCES users(id);

CREATE INDEX idx_listings_seller ON listings (seller_id);
CREATE INDEX idx_listings_seller_status ON listings (seller_id, status);
