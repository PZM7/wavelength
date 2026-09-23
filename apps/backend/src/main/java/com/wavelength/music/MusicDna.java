package com.wavelength.music;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

@Schema(requiredProperties = {"scores", "status", "topArtists"})
public record MusicDna(
        String status,
        @Nullable String archetype,
        Map<String, Double> scores,
        List<ArtistWeight> topArtists) {}
