CREATE TABLE spotify_listening_cache (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    top_artist_ids JSONB NOT NULL DEFAULT '[]',
    top_artist_names JSONB NOT NULL DEFAULT '[]',
    top_genres JSONB NOT NULL DEFAULT '[]',
    recently_played_artist_ids JSONB NOT NULL DEFAULT '[]',
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_spotify_listening_cache_user_id ON spotify_listening_cache(user_id);
