package com.wavelength.matching;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wavelength.music.TasteRepository;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

class MatchServiceTest {
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID x = UUID.randomUUID();
    private final UUID y = UUID.randomUUID();

    @Test
    void weightedJaccardUsesIntersectionOverUnion() {
        assertEquals(
                0.25,
                ArtistSimilarity.weightedJaccard(Map.of(x, 1.0, y, 1.0), Map.of(x, 0.5)),
                1e-12);
    }

    @Test
    void emptyDisjointIdenticalAndSymmetricSignals() {
        assertEquals(0.0, ArtistSimilarity.weightedJaccard(Map.of(), Map.of()));
        assertEquals(0.0, ArtistSimilarity.weightedJaccard(Map.of(x, 1.0), Map.of(y, 1.0)));
        assertEquals(1.0, ArtistSimilarity.weightedJaccard(Map.of(x, 0.4), Map.of(x, 0.4)));
        var left = Map.of(x, 0.8, y, 0.2);
        var right = Map.of(y, 0.6);
        assertEquals(
                ArtistSimilarity.weightedJaccard(left, right),
                ArtistSimilarity.weightedJaccard(right, left));
    }

    @Test
    void nonFiniteAndInvalidScoresRefused() {
        for (double score : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -0.1, 1.01}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ArtistSimilarity.weightedJaccard(Map.of(x, score), Map.of()));
        }
    }

    @Test
    void serviceCalculatesRealStoredSignals() {
        var taste = mock(TasteRepository.class);
        when(taste.weights(List.of(a, b))).thenReturn(Map.of(a, Map.of(x, 1.0), b, Map.of(x, 0.5)));
        assertEquals(
                0.5,
                new MatchService(taste, mock(MatchRepository.class)).calculateCompatibility(a, b));
    }

    @Test
    void limitAndCursorAreValidated() {
        var service = new MatchService(mock(TasteRepository.class), mock(MatchRepository.class));
        assertThrows(IllegalArgumentException.class, () -> service.findTopMatches(a, 51));
        assertThrows(IllegalArgumentException.class, () -> MatchCursor.decode("invalid"));
        var cursor = new MatchCursor(0.123456789, a);
        assertEquals(cursor, MatchCursor.decode(cursor.encode()));
    }
}
