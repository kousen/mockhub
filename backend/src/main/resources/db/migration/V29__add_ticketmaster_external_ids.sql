-- Add Ticketmaster external IDs for event and venue sync
ALTER TABLE events ADD COLUMN ticketmaster_event_id VARCHAR(100) UNIQUE;
CREATE INDEX idx_events_ticketmaster_id ON events (ticketmaster_event_id)
    WHERE ticketmaster_event_id IS NOT NULL;

ALTER TABLE venues ADD COLUMN ticketmaster_venue_id VARCHAR(100) UNIQUE;
CREATE INDEX idx_venues_ticketmaster_id ON venues (ticketmaster_venue_id)
    WHERE ticketmaster_venue_id IS NOT NULL;
