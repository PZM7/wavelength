package com.wavelength.social;

import com.wavelength.common.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ConnectionService {
    private final ConnectionRepository connections;
    private final PrivacyService privacy;
    private final JdbcTemplate jdbc;

    public ConnectionService(
            ConnectionRepository connections, PrivacyService privacy, JdbcTemplate jdbc) {
        this.connections = connections;
        this.privacy = privacy;
        this.jdbc = jdbc;
    }

    @Transactional
    public ConnectionResponse request(UUID viewer, UUID target) {
        if (viewer.equals(target)) throw new IllegalArgumentException();
        privacy.lockPair(viewer, target);
        privacy.requireVisible(viewer, target);
        boolean exists =
                Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                """
SELECT EXISTS(SELECT 1 FROM connections WHERE
    (requester_id = ? AND receiver_id = ?) OR (requester_id = ? AND receiver_id = ?))
""",
                                Boolean.class,
                                viewer,
                                target,
                                target,
                                viewer));
        if (exists)
            throw ApiException.conflict(
                    "CONNECTION_EXISTS", "A connection already exists for this pair");
        return ConnectionMapper.response(connections.saveAndFlush(new Connection(viewer, target)));
    }

    @Transactional
    public ConnectionResponse respond(UUID viewer, UUID id, ConnectionStatus status) {
        if (status == ConnectionStatus.PENDING) throw new IllegalArgumentException();
        // Read scalar IDs before acquiring the lock; do not cache a stale JPA entity.
        var pair =
                jdbc
                        .query(
                                "SELECT requester_id, receiver_id FROM connections WHERE id = ?",
                                (rs, index) ->
                                        new Participants(
                                                rs.getObject(1, UUID.class),
                                                rs.getObject(2, UUID.class)),
                                id)
                        .stream()
                        .findFirst()
                        .orElseThrow(ConnectionService::notFound);
        if (!viewer.equals(pair.receiver()))
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the receiver may respond");
        privacy.lockPair(pair.requester(), pair.receiver());
        if (privacy.blocked(pair.requester(), pair.receiver())) throw notFound();
        var connection = connections.findById(id).orElseThrow();
        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw ApiException.conflict(
                    "INVALID_CONNECTION_STATE", "Connection is no longer pending");
        }
        connection.setStatus(status);
        connection.setUpdatedAt(Instant.now());
        return ConnectionMapper.response(connections.saveAndFlush(connection));
    }

    @Transactional(readOnly = true)
    @Nullable
    public ConnectionResponse findPair(UUID viewer, UUID target) {
        if (viewer.equals(target)) throw new IllegalArgumentException();
        return jdbc.query(
                """
SELECT c.* FROM connections c WHERE
    ((c.requester_id = ? AND c.receiver_id = ?) OR (c.requester_id = ? AND c.receiver_id = ?))
    AND NOT EXISTS (SELECT 1 FROM blocks b WHERE
        (b.blocker_id = ? AND b.blocked_id = ?) OR
        (b.blocker_id = ? AND b.blocked_id = ?))
""",
                (rs, index) -> new ConnectionResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("requester_id", UUID.class),
                        rs.getObject("receiver_id", UUID.class),
                        ConnectionStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()),
                viewer, target, target, viewer, viewer, target, target, viewer)
                .stream().findFirst().orElse(null);
    }

    @Transactional(readOnly = true)
    public ConnectionPage list(UUID viewer, int limit, @Nullable UUID cursor) {
        if (limit < 1 || limit > 50) throw new IllegalArgumentException();
        var rows =
                jdbc.query(
                        """
SELECT c.* FROM connections c WHERE (c.requester_id = ? OR c.receiver_id = ?)
AND (?::uuid IS NULL OR c.id > ?::uuid)
AND NOT EXISTS (SELECT 1 FROM blocks b WHERE
    (b.blocker_id = c.requester_id AND b.blocked_id = c.receiver_id)
    OR (b.blocked_id = c.requester_id AND b.blocker_id = c.receiver_id))
ORDER BY c.id LIMIT ?
""",
                        (rs, index) ->
                                new ConnectionResponse(
                                        rs.getObject("id", UUID.class),
                                        rs.getObject("requester_id", UUID.class),
                                        rs.getObject("receiver_id", UUID.class),
                                        ConnectionStatus.valueOf(rs.getString("status")),
                                        rs.getTimestamp("created_at").toInstant(),
                                        rs.getTimestamp("updated_at").toInstant()),
                        viewer,
                        viewer,
                        cursor,
                        cursor,
                        limit + 1);
        return new ConnectionPage(
                rows.stream().limit(limit).toList(),
                rows.size() > limit ? rows.get(limit - 1).id() : null);
    }

    private static ApiException notFound() {
        return new ApiException(
                HttpStatus.NOT_FOUND, "CONNECTION_NOT_FOUND", "Connection not found");
    }

    private record Participants(UUID requester, UUID receiver) {}
}
