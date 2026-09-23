package com.wavelength.matching;

import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.UUID;

public record Match(
        UUID id,
        UUID userAId,
        UUID userBId,
        double compatibilityScore,
        @Nullable Double artistSimilarity,
        @Nullable Double trackSimilarity,
        @Nullable Double neighborhoodSimilarity,
        @Nullable Double recentSimilarity,
        @Nullable Double discoverySimilarity,
        Instant createdAt,
        Instant updatedAt) {}
