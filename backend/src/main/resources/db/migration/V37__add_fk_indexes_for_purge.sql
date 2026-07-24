-- FK indexes required by the dead-event purge bulk deletes. Without these,
-- every deleted listing/ticket forces PostgreSQL to sequentially scan the
-- referencing tables (price_history, order_items, cart_items) for FK checks.
CREATE INDEX IF NOT EXISTS idx_price_history_listing ON price_history (listing_id);
CREATE INDEX IF NOT EXISTS idx_order_items_listing ON order_items (listing_id);
CREATE INDEX IF NOT EXISTS idx_order_items_ticket ON order_items (ticket_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_listing ON cart_items (listing_id);
