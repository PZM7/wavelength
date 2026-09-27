package com.wavelength.music;

import com.wavelength.common.ApiException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SpotifyTasteSyncService {
    private final MusicAccountRepository accounts;
    private final SpotifyMusicProviderClient spotify;
    private final SpotifyTasteSyncPersistence persistence;

    public record SyncResponse(int artistsImported, int tracksImported, Instant syncedAt) {}
    public record ArtistScores(String providerId, String name, double shortTerm, double mediumTerm,
            double longTerm, String imageUrl, String spotifyUrl) {
        public ArtistScores(String providerId, String name, double shortTerm, double mediumTerm,
                double longTerm) {
            this(providerId, name, shortTerm, mediumTerm, longTerm, null, null);
        }
    }

    public SpotifyTasteSyncService(MusicAccountRepository accounts, SpotifyMusicProviderClient spotify,
            SpotifyTasteSyncPersistence persistence) {
        this.accounts = accounts;
        this.spotify = spotify;
        this.persistence = persistence;
    }

    public SyncResponse sync(UUID userId) {
        var account = accounts.findByUserIdAndProvider(userId, MusicProvider.SPOTIFY)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MUSIC_ACCOUNT_NOT_FOUND",
                        "Spotify is not connected"));
        Map<String, MutableScores> byProviderId = new LinkedHashMap<>();
        add(byProviderId, spotify.getTopArtists(account, "short_term"), 0);
        add(byProviderId, spotify.getTopArtists(account, "medium_term"), 1);
        add(byProviderId, spotify.getTopArtists(account, "long_term"), 2);
        var tracks = spotify.getTopTracks(account);
        var scores = byProviderId.values().stream()
                .map(a -> new ArtistScores(a.id, a.name, a.scores[0], a.scores[1], a.scores[2],
                        a.imageUrl, a.spotifyUrl))
                .toList();
        return persistence.replace(userId, account.getId(), scores, tracks);
    }

    private static void add(Map<String, MutableScores> result, List<ProviderArtistData> artists, int window) {
        for (int rank = 0; rank < artists.size(); rank++) {
            var item = artists.get(rank);
            var scores = result.computeIfAbsent(item.providerArtistId(),
                    id -> new MutableScores(id, item.name(), item.imageUrl(), item.spotifyUrl()));
            scores.scores[window] = Math.max(scores.scores[window], 1.0 - rank / 50.0);
            if (scores.imageUrl == null) scores.imageUrl = item.imageUrl();
            if (scores.spotifyUrl == null) scores.spotifyUrl = item.spotifyUrl();
        }
    }

    private static final class MutableScores {
        final String id;
        final String name;
        String imageUrl;
        String spotifyUrl;
        final double[] scores = new double[3];
        MutableScores(String id, String name, String imageUrl, String spotifyUrl) {
            this.id = id; this.name = name; this.imageUrl = imageUrl; this.spotifyUrl = spotifyUrl;
        }
    }
}
