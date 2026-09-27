package com.wavelength.music;

import com.wavelength.music.connect.MusicCredentialService;
import com.wavelength.music.connect.SpotifyGateway;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpotifyMusicProviderClient implements MusicProviderClient {
    private final MusicCredentialService credentials;
    private final SpotifyGateway spotify;

    public SpotifyMusicProviderClient(MusicCredentialService credentials, SpotifyGateway spotify) {
        this.credentials = credentials;
        this.spotify = spotify;
    }

    @Override
    public MusicProvider getProvider() {
        return MusicProvider.SPOTIFY;
    }

    @Override
    public boolean isStub() {
        return false;
    }

    @Override
    public List<ProviderArtistData> getTopArtists(MusicAccount account) {
        return getTopArtists(account, "medium_term");
    }

    public List<ProviderArtistData> getTopArtists(MusicAccount account, String timeRange) {
        return spotify.topArtists(credentials.accessTokenForSpotify(account.getUserId()), timeRange);
    }

    @Override
    public List<ProviderTrackData> getTopTracks(MusicAccount account) {
        return spotify.topTracks(credentials.accessTokenForSpotify(account.getUserId()));
    }

    @Override
    public List<ListeningData> getRecentListening(MusicAccount account) {
        return List.of();
    }
}
