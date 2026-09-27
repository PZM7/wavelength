package com.wavelength.music;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.lang.Nullable;

import java.util.UUID;

@Schema(requiredProperties = {"id", "name", "weight"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ArtistWeight(UUID id, String name, double weight,
        @Nullable String imageUrl, @Nullable String spotifyUrl) {
    public ArtistWeight(UUID id, String name, double weight) {
        this(id, name, weight, null, null);
    }
}
