CREATE TABLE matches (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_a_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_b_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    compatibility_score double precision NOT NULL CHECK (compatibility_score BETWEEN 0 AND 1),
    artist_similarity double precision CHECK (artist_similarity BETWEEN 0 AND 1),
    track_similarity double precision CHECK (track_similarity BETWEEN 0 AND 1),
    neighborhood_similarity double precision CHECK (neighborhood_similarity BETWEEN 0 AND 1),
    recent_similarity double precision CHECK (recent_similarity BETWEEN 0 AND 1),
    discovery_similarity double precision CHECK (discovery_similarity BETWEEN 0 AND 1),
    created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (user_a_id < user_b_id), UNIQUE (user_a_id, user_b_id)
);
CREATE INDEX matches_a_score_idx ON matches(user_a_id, compatibility_score DESC, user_b_id);
CREATE INDEX matches_b_score_idx ON matches(user_b_id, compatibility_score DESC, user_a_id);
