package com.wavelength.social;

import com.wavelength.common.ApiException;
import com.wavelength.users.PublicUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final JdbcTemplate jdbc;
    private final PrivacyService privacy;

    public ChatService(JdbcTemplate jdbc, PrivacyService privacy) {
        this.jdbc = jdbc;
        this.privacy = privacy;
    }

    @Transactional(readOnly = true)
    public ChatConversation conversation(UUID viewer, UUID id) {
        return jdbc.query("""
SELECT u.id, u.username, u.display_name, u.avatar_url
FROM connections c
JOIN users u ON u.id = CASE WHEN c.requester_id = ? THEN c.receiver_id ELSE c.requester_id END
WHERE c.id = ? AND (c.requester_id = ? OR c.receiver_id = ?)
AND c.status = 'ACCEPTED'
AND NOT EXISTS (SELECT 1 FROM blocks b WHERE
    (b.blocker_id = c.requester_id AND b.blocked_id = c.receiver_id) OR
    (b.blocked_id = c.requester_id AND b.blocker_id = c.receiver_id))
""", (rs, row) -> new ChatConversation(id, new PublicUser(rs.getObject("id", UUID.class),
                rs.getString("username"), rs.getString("display_name"), rs.getString("avatar_url"))),
                viewer, id, viewer, viewer).stream().findFirst().orElseThrow(ChatService::notFound);
    }

    @Transactional(readOnly = true)
    public ChatMessagePage messages(UUID viewer, UUID id, int limit, @Nullable Long before) {
        if (limit < 1 || limit > 100 || (before != null && before < 1)) throw new IllegalArgumentException();
        conversation(viewer, id);
        // Recheck access in the data query if a block committed after the metadata read.
        var rows = jdbc.query("""
SELECT m.id, m.sender_id, m.body, m.created_at
FROM chat_messages m JOIN connections c ON c.id = m.connection_id
WHERE c.id = ? AND (c.requester_id = ? OR c.receiver_id = ?)
AND c.status = 'ACCEPTED'
AND NOT EXISTS (SELECT 1 FROM blocks b WHERE
    (b.blocker_id = c.requester_id AND b.blocked_id = c.receiver_id) OR
    (b.blocked_id = c.requester_id AND b.blocker_id = c.receiver_id))
AND (?::bigint IS NULL OR m.id < ?)
ORDER BY m.id DESC LIMIT ?
""", (rs, row) -> new ChatMessage(rs.getString("id"), rs.getObject("sender_id", UUID.class),
                rs.getString("body"), rs.getTimestamp("created_at").toInstant()),
                id, viewer, viewer, before, before, limit + 1);
        String nextCursor = rows.size() > limit ? rows.get(limit - 1).id() : null;
        var chronological = new ArrayList<>(rows.stream().limit(limit).toList());
        Collections.reverse(chronological);
        return new ChatMessagePage(chronological, nextCursor);
    }

    @Transactional
    public ChatMessage send(UUID viewer, UUID id, ChatMessageRequest request) {
        var pair = jdbc.query("""
SELECT requester_id, receiver_id FROM connections
WHERE id = ? AND (requester_id = ? OR receiver_id = ?)
""", (rs, row) -> new Participants(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)),
                id, viewer, viewer).stream().findFirst().orElseThrow(ChatService::notFound);
        // The same pair locks as block/respond make a concurrent block revoke sending atomically.
        privacy.lockPair(pair.requester(), pair.receiver());
        conversation(viewer, id);
        String body = request.body().strip();
        jdbc.update("""
INSERT INTO chat_messages (connection_id, sender_id, client_message_id, body)
VALUES (?, ?, ?, ?) ON CONFLICT (connection_id, sender_id, client_message_id) DO NOTHING
""", id, viewer, request.clientMessageId(), body);
        var message = jdbc.queryForObject("""
SELECT id, sender_id, body, created_at FROM chat_messages
WHERE connection_id = ? AND sender_id = ? AND client_message_id = ?
""", (rs, row) -> new ChatMessage(rs.getString("id"), rs.getObject("sender_id", UUID.class),
                rs.getString("body"), rs.getTimestamp("created_at").toInstant()),
                id, viewer, request.clientMessageId());
        if (!message.body().equals(body))
            throw ApiException.conflict("MESSAGE_KEY_REUSED", "Message key already used for different content");
        return message;
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation not available");
    }

    private record Participants(UUID requester, UUID receiver) {}
}
