CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE music_accounts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider varchar(30) NOT NULL CHECK (provider IN ('SPOTIFY', 'APPLE_MUSIC')),
    provider_user_id varchar(255) NOT NULL,
    access_token_encrypted text,
    refresh_token_encrypted text,
    token_expires_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (user_id, provider), UNIQUE (provider, provider_user_id)
);
CREATE TABLE artists (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name varchar(255) NOT NULL,
    normalized_name varchar(255) NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX artists_normalized_name_idx ON artists(normalized_name);
CREATE TABLE tracks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), title varchar(255) NOT NULL,
    artist_id uuid NOT NULL REFERENCES artists(id), isrc varchar(12),
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (isrc IS NULL OR isrc ~ '^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$')
);
CREATE INDEX tracks_artist_idx ON tracks(artist_id);
CREATE INDEX tracks_isrc_idx ON tracks(isrc) WHERE isrc IS NOT NULL;
CREATE TABLE provider_artists (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), artist_id uuid NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    provider varchar(30) NOT NULL CHECK (provider IN ('SPOTIFY', 'APPLE_MUSIC')),
    provider_artist_id varchar(255) NOT NULL, UNIQUE (provider, provider_artist_id)
);
CREATE INDEX provider_artists_artist_idx ON provider_artists(artist_id);
CREATE TABLE provider_tracks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), track_id uuid NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    provider varchar(30) NOT NULL CHECK (provider IN ('SPOTIFY', 'APPLE_MUSIC')),
    provider_track_id varchar(255) NOT NULL, UNIQUE (provider, provider_track_id)
);
CREATE INDEX provider_tracks_track_idx ON provider_tracks(track_id);
CREATE TABLE user_artist_affinities (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    artist_id uuid NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    short_term_score double precision NOT NULL CHECK (short_term_score BETWEEN 0 AND 1),
    medium_term_score double precision NOT NULL CHECK (medium_term_score BETWEEN 0 AND 1),
    long_term_score double precision NOT NULL CHECK (long_term_score BETWEEN 0 AND 1),
    updated_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(user_id, artist_id)
);
CREATE INDEX user_artist_affinities_artist_idx ON user_artist_affinities(artist_id, user_id);
CREATE TABLE user_track_affinities (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    track_id uuid NOT NULL REFERENCES tracks(id) ON DELETE CASCADE,
    short_term_score double precision NOT NULL CHECK (short_term_score BETWEEN 0 AND 1),
    medium_term_score double precision NOT NULL CHECK (medium_term_score BETWEEN 0 AND 1),
    long_term_score double precision NOT NULL CHECK (long_term_score BETWEEN 0 AND 1),
    updated_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(user_id, track_id)
);
CREATE INDEX user_track_affinities_track_idx ON user_track_affinities(track_id, user_id);
-- No dimensions/index until a versioned embedding model has been selected.
CREATE TABLE user_taste_embeddings (
    user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    embedding vector NOT NULL, model_version varchar(100) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);
