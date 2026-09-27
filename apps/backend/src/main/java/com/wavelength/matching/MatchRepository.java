package com.wavelength.matching;

import com.wavelength.users.PublicUser;

import org.jooq.DSLContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class MatchRepository {
    private final DSLContext db;

    public MatchRepository(DSLContext db) {
        this.db = db;
    }

    // Rank all eligible candidates before LIMIT. Bind parameters; no N+1 reads.
    public List<MatchResult> find(UUID userId, int limit, @Nullable MatchCursor cursor) {
        Double score = cursor == null ? null : cursor.score();
        UUID id = cursor == null ? null : cursor.id();
        return db.resultQuery(
                        """
WITH signals AS (
    SELECT ma.user_id, aa.artist_id,
           (aa.short_term_score + aa.medium_term_score + aa.long_term_score) / 3.0 AS w
    FROM music_account_artist_affinities aa
    JOIN music_accounts ma ON ma.id = aa.music_account_id
), weights AS (
    SELECT user_id, artist_id, max(w) AS w FROM signals GROUP BY user_id, artist_id
), mine AS (SELECT artist_id, w FROM weights WHERE user_id = ? AND w > 0),
totals AS (SELECT user_id, sum(w) AS total FROM weights GROUP BY user_id),
shared_weights AS (
    SELECT w.user_id, sum(least(w.w, m.w)) AS intersection, count(*) AS shared
    FROM weights w JOIN mine m USING (artist_id) WHERE w.w > 0 GROUP BY w.user_id
), ranked AS (
    SELECT u.id, u.username, u.display_name, u.avatar_url, o.shared,
        o.intersection / nullif(t.total + (SELECT coalesce(sum(w), 0) FROM mine) - o.intersection, 0) AS score
    FROM users u JOIN shared_weights o ON o.user_id = u.id JOIN totals t ON t.user_id = u.id
    WHERE u.id <> ? AND u.discoverable AND NOT EXISTS (
        SELECT 1 FROM blocks b WHERE (b.blocker_id = ? AND b.blocked_id = u.id)
            OR (b.blocked_id = ? AND b.blocker_id = u.id)
    )
)
SELECT * FROM ranked WHERE score > 0
    AND (?::double precision IS NULL OR score < ? OR (score = ? AND id > ?::uuid))
ORDER BY score DESC, id LIMIT ?
""",
                        userId,
                        userId,
                        userId,
                        userId,
                        score,
                        score,
                        score,
                        id,
                        limit)
                .fetch(
                        row -> {
                            int count = row.get("shared", Integer.class);
                            return new MatchResult(
                                    new PublicUser(
                                            row.get("id", UUID.class),
                                            row.get("username", String.class),
                                            row.get("display_name", String.class),
                                            row.get("avatar_url", String.class)),
                                    row.get("score", Double.class),
                                    List.of(
                                            new MatchReason(
                                                    "SHARED_ARTISTS",
                                                    count == 1 ? "1 artista en común" : count + " artistas en común",
                                                    count)));
                        });
    }
}
