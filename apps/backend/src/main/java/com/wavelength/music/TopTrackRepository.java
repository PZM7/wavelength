package com.wavelength.music;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TopTrackRepository {
    private final JdbcTemplate jdbc;

    public TopTrackRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<TopTrack> forUser(UUID userId) {
        return jdbc.query("""
                SELECT t.provider_track_id, t.title, t.artist_name, t.rank, t.image_url, t.spotify_url
                FROM music_account_top_tracks t
                JOIN music_accounts a ON a.id = t.music_account_id
                WHERE a.user_id = ? AND a.provider = 'SPOTIFY'
                ORDER BY t.rank, t.provider_track_id LIMIT 20
                """, (rs, row) -> new TopTrack(rs.getString("provider_track_id"),
                rs.getString("title"), rs.getString("artist_name"), rs.getInt("rank"),
                rs.getString("image_url"), rs.getString("spotify_url")), userId);
    }
}
