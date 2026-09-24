CREATE TABLE user_manual_artist_preferences (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    artist_id uuid NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, artist_id)
);
CREATE INDEX user_manual_artist_preferences_artist_idx
    ON user_manual_artist_preferences(artist_id, user_id);
