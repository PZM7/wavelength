package com.wavelength.music;

import java.time.Instant;
import java.util.UUID;

public record UserTrackAffinity(
        UUID userId,
        UUID trackId,
        double shortTermScore,
        double mediumTermScore,
        double longTermScore,
        Instant updatedAt) {}
