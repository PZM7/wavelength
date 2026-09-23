package com.wavelength.matching;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

public final class ArtistSimilarity {
    private ArtistSimilarity() {}

    public static double weightedJaccard(Map<UUID, Double> a, Map<UUID, Double> b) {
        for (var values : java.util.List.of(a.values(), b.values())) {
            for (double score : values) {
                if (!Double.isFinite(score) || score < 0.0 || score > 1.0)
                    throw new IllegalArgumentException();
            }
        }
        // Preserve Kotlin's insertion order and ordinary (not compensated) double summation.
        var keys = new LinkedHashSet<>(a.keySet());
        keys.addAll(b.keySet());
        double union = 0.0;
        for (UUID key : keys) union += Math.max(a.getOrDefault(key, 0.0), b.getOrDefault(key, 0.0));
        if (union == 0.0) return 0.0;
        double intersection = 0.0;
        for (UUID key : keys)
            intersection += Math.min(a.getOrDefault(key, 0.0), b.getOrDefault(key, 0.0));
        return intersection / union;
    }
}
