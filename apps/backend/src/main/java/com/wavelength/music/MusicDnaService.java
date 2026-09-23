package com.wavelength.music;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class MusicDnaService {
    private final TasteRepository taste;

    public MusicDnaService(TasteRepository taste) {
        this.taste = taste;
    }

    public MusicDna get(UUID userId) {
        var artists = taste.artists(userId);
        // Dimensions remain absent until there is defensible evidence for them.
        return new MusicDna(
                artists.isEmpty() ? "INSUFFICIENT_DATA" : "ARTIST_SIGNALS_ONLY",
                null,
                Map.of(),
                artists.stream().limit(20).toList());
    }
}
