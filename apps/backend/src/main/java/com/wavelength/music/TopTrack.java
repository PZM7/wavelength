package com.wavelength.music;

import org.springframework.lang.Nullable;

public record TopTrack(String id, String title, String artistName, int rank,
        @Nullable String imageUrl, @Nullable String spotifyUrl) {}
