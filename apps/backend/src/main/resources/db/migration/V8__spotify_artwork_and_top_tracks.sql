ALTER TABLE provider_artists ADD COLUMN image_url text;
ALTER TABLE provider_artists ADD COLUMN spotify_url text;

CREATE TABLE music_account_top_tracks (
    music_account_id uuid NOT NULL REFERENCES music_accounts(id) ON DELETE CASCADE,
    provider_track_id varchar(255) NOT NULL,
    title varchar(255) NOT NULL,
    artist_name varchar(255) NOT NULL,
    image_url text,
    spotify_url text,
    rank integer NOT NULL CHECK (rank BETWEEN 1 AND 50),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (music_account_id, provider_track_id)
);
CREATE INDEX music_account_top_tracks_rank_idx
    ON music_account_top_tracks (music_account_id, rank);
