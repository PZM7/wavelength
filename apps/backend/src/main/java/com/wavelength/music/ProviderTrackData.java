package com.wavelength.music;

import org.springframework.lang.Nullable;

public record ProviderTrackData(
        String providerTrackId, String title, ProviderArtistData artist, @Nullable String isrc) {}
