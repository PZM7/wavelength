package com.wavelength.music;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(requiredProperties = {"createdAt", "id", "provider"})
public record MusicAccountResponse(UUID id, MusicProvider provider, Instant createdAt) {}
