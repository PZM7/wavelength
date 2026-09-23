package com.wavelength.music;

import java.time.Instant;
import java.util.UUID;

public record UserArtistAffinity(
        UUID userId,
        UUID artistId,
        double shortTermScore,
        double mediumTermScore,
        double longTermScore,
        Instant updatedAt) {}
