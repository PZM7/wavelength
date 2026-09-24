package com.wavelength.music;

import com.wavelength.common.TextInput;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ManualTasteService {
    private static final int MAX_ARTISTS = 20;
    private final JdbcTemplate jdbc;

    public ManualTasteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<ArtistChoice> selected(UUID userId) {
        return jdbc.query(
                """
                SELECT a.id, a.name FROM user_manual_artist_preferences p
                JOIN artists a ON a.id = p.artist_id
                WHERE p.user_id = ? ORDER BY a.name, a.id
                """,
                (rs, row) -> new ArtistChoice(rs.getObject("id", UUID.class), rs.getString("name")),
                userId);
    }

    @Transactional(readOnly = true)
    public List<ArtistChoice> search(String query) {
        String term = normalized(query == null ? "" : query);
        if (term.length() > 100) throw new IllegalArgumentException();
        String pattern = "%" + term.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_") + "%";
        return jdbc.query(
                """
                SELECT id, name FROM artists WHERE normalized_name LIKE ? ESCAPE '\\'
                ORDER BY name, id LIMIT 20
                """,
                (rs, row) -> new ArtistChoice(rs.getObject("id", UUID.class), rs.getString("name")),
                pattern);
    }

    @Transactional
    public List<ArtistChoice> replace(UUID userId, FavoriteArtistsRequest request) {
        if (request == null || request.names() == null || request.names().size() > MAX_ARTISTS)
            throw new IllegalArgumentException();
        var canonicalNames = new ArrayList<String>();
        var normalizedNames = new HashSet<String>();
        for (String raw : request.names()) {
            if (raw == null) throw new IllegalArgumentException();
            String name = clean(raw);
            String key = normalized(name);
            if (name.isEmpty() || name.length() > 100 || name.codePoints().anyMatch(Character::isISOControl)
                    || !normalizedNames.add(key)) throw new IllegalArgumentException();
            canonicalNames.add(name);
        }
        canonicalNames.sort(Comparator.comparing(ManualTasteService::normalized));

        var artists = new ArrayList<ArtistChoice>();
        for (String name : canonicalNames) {
            String key = normalized(name);
            // Serialize creation of the same manually named artist across users.
            jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", Object.class, key);
            var existing = jdbc.query(
                    "SELECT id, name FROM artists WHERE normalized_name = ? ORDER BY created_at, id LIMIT 1",
                    (rs, row) -> new ArtistChoice(rs.getObject("id", UUID.class), rs.getString("name")),
                    key);
            if (!existing.isEmpty()) {
                artists.add(existing.getFirst());
            } else {
                UUID id = UUID.randomUUID();
                jdbc.update("INSERT INTO artists (id, name, normalized_name) VALUES (?, ?, ?)", id, name, key);
                artists.add(new ArtistChoice(id, name));
            }
        }

        jdbc.update("DELETE FROM user_manual_artist_preferences WHERE user_id = ?", userId);
        for (ArtistChoice artist : artists) {
            jdbc.update("INSERT INTO user_manual_artist_preferences (user_id, artist_id) VALUES (?, ?)",
                    userId, artist.id());
        }
        return selected(userId);
    }

    private static String clean(String value) {
        return TextInput.trim(Normalizer.normalize(value, Normalizer.Form.NFKC))
                .replaceAll("[\\p{Z}\\s]+", " ");
    }

    private static String normalized(String value) {
        return clean(value).toLowerCase(Locale.ROOT);
    }
}
