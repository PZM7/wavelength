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
SELECT a.id, a.name, (f.short_term_score + f.medium_term_score + f.long_term_score) / 3.0 AS weight
FROM user_artist_affinities f JOIN artists a ON a.id = f.artist_id
WHERE f.user_id = ? ORDER BY weight DESC, a.id LIMIT 200
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
SELECT user_id, artist_id, (short_term_score + medium_term_score + long_term_score) / 3.0 AS weight
FROM user_artist_affinities WHERE user_id IN (%s)
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
