CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    external_auth_id varchar(255) NOT NULL UNIQUE,
    username varchar(30) UNIQUE,
    display_name varchar(80),
    avatar_url varchar(2048),
    city varchar(120),
    birth_date date,
    discoverable boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT valid_username CHECK (username IS NULL OR username ~ '^[a-z0-9_]{3,30}$')
);
CREATE INDEX users_discoverable_id_idx ON users (id) WHERE discoverable;
