package com.wavelength.music;

import org.springframework.lang.Nullable;

public record ProviderArtistData(String providerArtistId, String name,
        @Nullable String imageUrl, @Nullable String spotifyUrl) {
    public ProviderArtistData(String providerArtistId, String name) {
        this(providerArtistId, name, null, null);
    }
}
