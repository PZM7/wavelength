package com.wavelength.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "DEV_SEED_ENABLED", havingValue = "true")
public class DevSeed implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final String marcSubject;

    public DevSeed(
            JdbcTemplate jdbc, @Value("${DEV_SEED_AUTH_SUBJECT:dev|marc}") String marcSubject) {
        this.jdbc = jdbc;
        this.marcSubject = marcSubject;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var names = List.of("Marc", "Lucía", "Alex", "Nora");
        var handles = List.of("marc", "lucia", "alex", "nora");
        var artists =
                List.of(
                        "Frank Ocean",
                        "Sampha",
                        "Fred again..",
                        "Ralphie Choo",
                        "Bicep",
                        "Jamie xx",
                        "Judeline");
        var seededUsers = new ArrayList<UUID>();
        for (int index = 0; index < names.size(); index++) {
            String subject = index == 0 ? marcSubject : "dev|" + handles.get(index);
            jdbc.update(
                    """
                    INSERT INTO users (id, external_auth_id, username, display_name, discoverable)
                    VALUES (?, ?, ?, ?, true) ON CONFLICT DO NOTHING
                    """,
                    userId(index),
                    subject,
                    handles.get(index),
                    names.get(index));
            seededUsers.add(
                    jdbc
                            .query(
                                    "SELECT id FROM users WHERE external_auth_id = ?",
                                    (rs, row) -> rs.getObject(1, UUID.class),
                                    subject)
                            .stream()
                            .findFirst()
                            .orElse(null));
        }
        for (int index = 0; index < artists.size(); index++) {
            String name = artists.get(index);
            jdbc.update(
                    "INSERT INTO artists (id, name, normalized_name) VALUES (?, ?, ?) ON CONFLICT"
                            + " DO NOTHING",
                    artistId(index),
                    name,
                    name.toLowerCase(Locale.ROOT));
        }
        double[][] signals = {
            {0.95, 0.9, 0.7, 0.65, 0.5, 0.8, 0.6},
            {0.9, 0.85, 0.6, 0.7, 0.45, 0.75, 0.65},
            {0.2, 0.3, 0.95, 0.0, 0.9, 0.9, 0.1},
            {0.3, 0.4, 0.1, 0.9, 0.0, 0.2, 0.95}
        };
        for (int user = 0; user < signals.length; user++) {
            UUID targetId = seededUsers.get(user);
            if (targetId == null) continue;
            for (int artist = 0; artist < signals[user].length; artist++) {
                double weight = signals[user][artist];
                jdbc.update(
                        """
INSERT INTO user_artist_affinities (user_id, artist_id, short_term_score, medium_term_score, long_term_score)
VALUES (?, ?, ?, ?, ?) ON CONFLICT DO NOTHING
""",
                        targetId,
                        artistId(artist),
                        weight,
                        weight,
                        weight);
            }
        }
    }

    public static UUID userId(int index) {
        return UUID.fromString("00000000-0000-0000-0000-00000000010" + (index + 1));
    }

    public static UUID artistId(int index) {
        return UUID.fromString("00000000-0000-0000-0000-00000000020" + (index + 1));
    }
}
