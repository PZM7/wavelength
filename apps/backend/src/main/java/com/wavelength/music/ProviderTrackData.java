package com.wavelength.music;

import org.springframework.lang.Nullable;

public record ProviderTrackData(
        String providerTrackId, String title, ProviderArtistData artist, @Nullable String isrc,
        @Nullable String imageUrl, @Nullable String spotifyUrl) {
    public ProviderTrackData(String providerTrackId, String title, ProviderArtistData artist,
            @Nullable String isrc) {
        this(providerTrackId, title, artist, isrc, null, null);
    }
}
