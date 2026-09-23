package com.wavelength.social;

import com.wavelength.common.ApiException;
import com.wavelength.users.PublicUser;
import com.wavelength.users.UserMapper;
import com.wavelength.users.UserRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PrivacyService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;

    public PrivacyService(JdbcTemplate jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    // Shared PostgreSQL UUID lock order serializes all block/connection mutations.
    public void lockPair(UUID a, UUID b) {
        if (a.equals(b)) throw new IllegalArgumentException();
        int count =
                jdbc.queryForList(
                                "SELECT id FROM users WHERE id IN (?, ?) ORDER BY id FOR UPDATE",
                                a,
                                b)
                        .size();
        if (count != 2) throw ApiException.userNotFound();
    }

    public boolean blocked(UUID a, UUID b) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        """
SELECT EXISTS(SELECT 1 FROM blocks WHERE (blocker_id = ? AND blocked_id = ?) OR (blocker_id = ? AND blocked_id = ?))
""",
                        Boolean.class,
                        a,
                        b,
                        b,
                        a));
    }

    public void requireVisible(UUID viewer, UUID target) {
        if (blocked(viewer, target)) throw ApiException.userNotFound();
        var user = users.findById(target).orElseThrow(ApiException::userNotFound);
        boolean connected =
                Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                """
SELECT EXISTS(SELECT 1 FROM connections WHERE status = 'ACCEPTED'
    AND ((requester_id = ? AND receiver_id = ?) OR (requester_id = ? AND receiver_id = ?)))
""",
                                Boolean.class,
                                viewer,
                                target,
                                target,
                                viewer));
        if (!viewer.equals(target) && !user.isDiscoverable() && !connected)
            throw ApiException.userNotFound();
    }

    @Transactional(readOnly = true)
    public PublicUser profile(UUID viewer, UUID target) {
        requireVisible(viewer, target);
        return UserMapper.publicProfile(
                users.findById(target).orElseThrow(ApiException::userNotFound));
    }

    @Transactional
    public void block(UUID viewer, UUID target) {
        lockPair(viewer, target);
        jdbc.update(
                "INSERT INTO blocks (blocker_id, blocked_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                viewer,
                target);
        jdbc.update(
                """
UPDATE connections SET status = 'REJECTED', updated_at = now()
WHERE (requester_id = ? AND receiver_id = ?) OR (requester_id = ? AND receiver_id = ?)
""",
                viewer,
                target,
                target,
                viewer);
    }

    @Transactional
    public void unblock(UUID viewer, UUID target) {
        lockPair(viewer, target);
        jdbc.update("DELETE FROM blocks WHERE blocker_id = ? AND blocked_id = ?", viewer, target);
    }
}
