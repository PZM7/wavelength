package com.wavelength.music;

import org.springframework.stereotype.Component;

import java.util.List;

// Explicit empty stub: no OAuth, network calls, tokens or invented live listening.
@Component
public class AppleMusicProviderClient implements MusicProviderClient {
    @Override
    public MusicProvider getProvider() {
        return MusicProvider.APPLE_MUSIC;
    }

    @Override
    public boolean isStub() {
        return true;
    }

    @Override
    public List<ProviderArtistData> getTopArtists(MusicAccount account) {
        return List.of();
    }

    @Override
    public List<ProviderTrackData> getTopTracks(MusicAccount account) {
        return List.of();
    }

    @Override
    public List<ListeningData> getRecentListening(MusicAccount account) {
        return List.of();
    }
}
