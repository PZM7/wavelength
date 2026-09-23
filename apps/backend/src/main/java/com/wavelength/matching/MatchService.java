package com.wavelength.matching;

import com.wavelength.music.TasteRepository;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MatchService {
    private final TasteRepository taste;
    private final MatchRepository matches;

    public MatchService(TasteRepository taste, MatchRepository matches) {
        this.taste = taste;
        this.matches = matches;
    }

    public double calculateCompatibility(UUID userA, UUID userB) {
        var weights = taste.weights(List.of(userA, userB));
        return ArtistSimilarity.weightedJaccard(
                weights.getOrDefault(userA, Map.of()), weights.getOrDefault(userB, Map.of()));
    }

    public MatchPage findTopMatches(UUID userId, int limit) {
        return findTopMatches(userId, limit, null);
    }

    public MatchPage findTopMatches(UUID userId, int limit, @Nullable String cursor) {
        if (limit < 1 || limit > 50) throw new IllegalArgumentException();
        var rows =
                matches.find(userId, limit + 1, cursor == null ? null : MatchCursor.decode(cursor));
        var page = List.copyOf(rows.subList(0, Math.min(limit, rows.size())));
        String next = null;
        if (rows.size() > limit) {
            var last = page.getLast();
            next = new MatchCursor(last.compatibility(), last.user().id()).encode();
        }
        return new MatchPage(page, next);
    }
}
