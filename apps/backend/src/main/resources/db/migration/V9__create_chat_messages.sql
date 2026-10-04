CREATE TABLE chat_messages (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    connection_id uuid NOT NULL REFERENCES connections(id) ON DELETE CASCADE,
    sender_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_message_id varchar(80) NOT NULL,
    body text NOT NULL CHECK (char_length(body) BETWEEN 1 AND 2000 AND body !~ '^[[:space:]]*$'),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (connection_id, sender_id, client_message_id)
);
CREATE INDEX chat_messages_connection_id_idx ON chat_messages (connection_id, id DESC);
