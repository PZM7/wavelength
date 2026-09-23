CREATE TABLE connections (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status varchar(20) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (requester_id <> receiver_id)
);
-- One pair for its entire lifecycle, even if requests arrive in opposite directions.
CREATE UNIQUE INDEX connections_pair_idx ON connections(least(requester_id, receiver_id), greatest(requester_id, receiver_id));
CREATE INDEX connections_requester_idx ON connections(requester_id, id);
CREATE INDEX connections_receiver_idx ON connections(receiver_id, id);
CREATE TABLE blocks (
    blocker_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);
CREATE INDEX blocks_blocked_idx ON blocks(blocked_id, blocker_id);
CREATE TABLE reports (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason varchar(30) NOT NULL CHECK (reason IN ('SPAM', 'HARASSMENT', 'IMPERSONATION', 'OTHER')),
    description varchar(2000), created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (reporter_id <> reported_user_id)
);
CREATE INDEX reports_reported_idx ON reports(reported_user_id, created_at DESC);
CREATE INDEX reports_reporter_idx ON reports(reporter_id);
