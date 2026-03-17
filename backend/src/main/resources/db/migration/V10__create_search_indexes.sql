-- Function to auto-update search_vector from event fields
CREATE OR REPLACE FUNCTION events_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.artist_name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update search_vector on INSERT or UPDATE
CREATE TRIGGER trg_events_search_vector
    BEFORE INSERT OR UPDATE OF name, artist_name, description
    ON events
    FOR EACH ROW
    EXECUTE FUNCTION events_search_vector_update();
