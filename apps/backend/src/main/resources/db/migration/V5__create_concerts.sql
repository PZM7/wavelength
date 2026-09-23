CREATE TABLE concerts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(), artist_id uuid NOT NULL REFERENCES artists(id),
    name varchar(255) NOT NULL, venue_name varchar(255) NOT NULL,
    city varchar(120) NOT NULL, country varchar(2) NOT NULL CHECK (country ~ '^[A-Z]{2}$'),
    starts_at timestamptz NOT NULL, provider varchar(50) NOT NULL,
    provider_event_id varchar(255) NOT NULL, UNIQUE(provider, provider_event_id)
);
CREATE INDEX concerts_artist_starts_idx ON concerts(artist_id, starts_at);
CREATE INDEX concerts_city_starts_idx ON concerts(city, starts_at);
CREATE TABLE concert_attendances (
    concert_id uuid NOT NULL REFERENCES concerts(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status varchar(30) NOT NULL CHECK (status IN ('INTERESTED', 'GOING', 'LOOKING_FOR_PEOPLE', 'GOING_WITH_GROUP')),
    visibility varchar(20) NOT NULL DEFAULT 'PRIVATE' CHECK (visibility IN ('PRIVATE', 'CONNECTIONS', 'PUBLIC')),
    PRIMARY KEY(concert_id, user_id)
);
CREATE INDEX concert_attendances_user_idx ON concert_attendances(user_id, concert_id);
