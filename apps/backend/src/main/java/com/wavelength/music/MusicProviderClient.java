package com.wavelength.music;

import java.util.List;

public interface MusicProviderClient {
    MusicProvider getProvider();

    boolean isStub();

    List<ProviderArtistData> getTopArtists(MusicAccount account);

    List<ProviderTrackData> getTopTracks(MusicAccount account);

    List<ListeningData> getRecentListening(MusicAccount account);
}
