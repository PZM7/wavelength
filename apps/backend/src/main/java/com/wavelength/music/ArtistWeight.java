package com.wavelength.music;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(requiredProperties = {"id", "name", "weight"})
public record ArtistWeight(UUID id, String name, double weight) {}
