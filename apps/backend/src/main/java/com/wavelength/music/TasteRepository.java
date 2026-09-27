package com.wavelength.music;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class TasteRepository {
    private final DSLContext db;

    public TasteRepository(DSLContext db) {
        this.db = db;
    }

    public List<ArtistWeight> artists(UUID userId) {
        return db.resultQuery(
                        """
WITH signals AS (
    SELECT user_id, artist_id, (short_term_score + medium_term_score + long_term_score) / 3.0 AS weight
    FROM user_artist_affinities
    UNION ALL
    SELECT ma.user_id, aa.artist_id,
           (aa.short_term_score + aa.medium_term_score + aa.long_term_score) / 3.0 AS weight
    FROM music_account_artist_affinities aa
    JOIN music_accounts ma ON ma.id = aa.music_account_id
    UNION ALL
    SELECT user_id, artist_id, 1.0 AS weight FROM user_manual_artist_preferences
), effective AS (
    SELECT artist_id, max(weight) AS weight FROM signals WHERE user_id = ? GROUP BY artist_id
)
SELECT a.id, a.name, e.weight FROM effective e JOIN artists a ON a.id = e.artist_id
ORDER BY e.weight DESC, a.name, a.id LIMIT 200
""",
                        userId)
                .fetch(
                        row ->
                                new ArtistWeight(
                                        row.get("id", UUID.class),
                                        row.get("name", String.class),
                                        row.get("weight", Double.class)));
    }

    public Map<UUID, Map<UUID, Double>> weights(List<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
        var rows =
                db.resultQuery(
                                """
WITH signals AS (
    SELECT user_id, artist_id, (short_term_score + medium_term_score + long_term_score) / 3.0 AS weight
    FROM user_artist_affinities
    UNION ALL
    SELECT ma.user_id, aa.artist_id,
           (aa.short_term_score + aa.medium_term_score + aa.long_term_score) / 3.0 AS weight
    FROM music_account_artist_affinities aa
    JOIN music_accounts ma ON ma.id = aa.music_account_id
    UNION ALL
    SELECT user_id, artist_id, 1.0 AS weight FROM user_manual_artist_preferences
)
SELECT user_id, artist_id, max(weight) AS weight FROM signals
WHERE user_id IN (%s) GROUP BY user_id, artist_id
"""
                                        .formatted(placeholders),
                                userIds.toArray())
                        .fetch();
        Map<UUID, Map<UUID, Double>> result = new LinkedHashMap<>();
        for (var row : rows) {
            result.computeIfAbsent(row.get("user_id", UUID.class), ignored -> new LinkedHashMap<>())
                    .put(row.get("artist_id", UUID.class), row.get("weight", Double.class));
        }
        return result;
    }
}
