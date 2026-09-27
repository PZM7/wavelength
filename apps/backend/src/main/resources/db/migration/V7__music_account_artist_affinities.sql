CREATE TABLE music_account_artist_affinities (
    music_account_id uuid NOT NULL REFERENCES music_accounts(id) ON DELETE CASCADE,
    artist_id uuid NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    short_term_score double precision NOT NULL CHECK (short_term_score BETWEEN 0 AND 1),
    medium_term_score double precision NOT NULL CHECK (medium_term_score BETWEEN 0 AND 1),
    long_term_score double precision NOT NULL CHECK (long_term_score BETWEEN 0 AND 1),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (music_account_id, artist_id)
);
CREATE INDEX music_account_artist_affinities_artist_idx
    ON music_account_artist_affinities (artist_id, music_account_id);
