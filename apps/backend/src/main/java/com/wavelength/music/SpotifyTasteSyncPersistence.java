package com.wavelength.music;

import com.wavelength.common.ApiException;
import java.text.Normalizer;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpotifyTasteSyncPersistence {
    private final JdbcTemplate jdbc;
    private final MusicAccountRepository accounts;

    public SpotifyTasteSyncPersistence(JdbcTemplate jdbc, MusicAccountRepository accounts) {
        this.jdbc = jdbc;
        this.accounts = accounts;
    }

    @Transactional
    public SpotifyTasteSyncService.SyncResponse replace(UUID userId, UUID accountId,
            List<SpotifyTasteSyncService.ArtistScores> artists, List<ProviderTrackData> tracks) {
        var account = accounts.lockByUserIdAndProvider(userId, MusicProvider.SPOTIFY)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "MUSIC_RECONNECT_REQUIRED",
                        "Reconnect Spotify to continue"));
        if (!account.getId().equals(accountId))
            throw new ApiException(HttpStatus.CONFLICT, "MUSIC_RECONNECT_REQUIRED",
                    "Spotify account changed during sync; try again");
        jdbc.update("DELETE FROM music_account_artist_affinities WHERE music_account_id = ?", accountId);
        jdbc.update("DELETE FROM music_account_top_tracks WHERE music_account_id = ?", accountId);
        Instant now = Instant.now();
        for (var artist : artists) {
            UUID artistId = resolveArtist(artist.providerId(), artist.name(),
                    artist.imageUrl(), artist.spotifyUrl());
            jdbc.update("""
                    INSERT INTO music_account_artist_affinities
                        (music_account_id, artist_id, short_term_score, medium_term_score, long_term_score, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (music_account_id, artist_id) DO UPDATE SET
                        short_term_score = greatest(music_account_artist_affinities.short_term_score, excluded.short_term_score),
                        medium_term_score = greatest(music_account_artist_affinities.medium_term_score, excluded.medium_term_score),
                        long_term_score = greatest(music_account_artist_affinities.long_term_score, excluded.long_term_score),
                        updated_at = excluded.updated_at
                    """, accountId, artistId, artist.shortTerm(), artist.mediumTerm(),
                    artist.longTerm(), Timestamp.from(now));
        }
        for (int rank = 0; rank < tracks.size(); rank++) {
            var track = tracks.get(rank);
            jdbc.update("""
                    INSERT INTO music_account_top_tracks
                        (music_account_id, provider_track_id, title, artist_name, image_url,
                         spotify_url, rank, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (music_account_id, provider_track_id) DO UPDATE SET
                        title = excluded.title, artist_name = excluded.artist_name,
                        image_url = excluded.image_url, spotify_url = excluded.spotify_url,
                        rank = least(music_account_top_tracks.rank, excluded.rank),
                        updated_at = excluded.updated_at
                    """, accountId, track.providerTrackId(), track.title(), track.artist().name(),
                    track.imageUrl(), track.spotifyUrl(), rank + 1, Timestamp.from(now));
        }
        return new SpotifyTasteSyncService.SyncResponse(artists.size(), tracks.size(), now);
    }

    private UUID resolveArtist(String providerId, String name, String imageUrl, String spotifyUrl) {
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", Object.class,
                "SPOTIFY:" + providerId);
        var mapped = jdbc.query("""
                SELECT artist_id FROM provider_artists WHERE provider = 'SPOTIFY' AND provider_artist_id = ?
                """, (rs, row) -> rs.getObject(1, UUID.class), providerId);
        if (!mapped.isEmpty()) {
            jdbc.update("""
                    UPDATE provider_artists SET image_url = coalesce(?, image_url),
                        spotify_url = coalesce(?, spotify_url)
                    WHERE provider = 'SPOTIFY' AND provider_artist_id = ?
                    """, imageUrl, spotifyUrl, providerId);
            return mapped.getFirst();
        }

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKC)
                .strip().replaceAll("[\\p{Z}\\s]+", " ").toLowerCase(Locale.ROOT);
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", Object.class,
                "artist-name:" + normalized);
        var existing = jdbc.query("""
                SELECT id FROM artists WHERE normalized_name = ? ORDER BY created_at, id LIMIT 1
                """, (rs, row) -> rs.getObject(1, UUID.class), normalized);
        UUID id;
        if (existing.isEmpty()) {
            id = UUID.randomUUID();
            jdbc.update("INSERT INTO artists (id, name, normalized_name) VALUES (?, ?, ?)",
                    id, name, normalized);
        } else id = existing.getFirst();
        jdbc.update("""
                INSERT INTO provider_artists (artist_id, provider, provider_artist_id, image_url, spotify_url)
                VALUES (?, 'SPOTIFY', ?, ?, ?)
                """, id, providerId, imageUrl, spotifyUrl);
        return id;
    }
}
