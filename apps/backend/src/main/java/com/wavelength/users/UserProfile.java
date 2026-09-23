package com.wavelength.users;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.lang.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(requiredProperties = {"createdAt", "discoverable", "id", "updatedAt"})
public record UserProfile(
        UUID id,
        @Nullable String username,
        @Nullable String displayName,
        @Nullable String avatarUrl,
        @Nullable String city,
        @Nullable LocalDate birthDate,
        boolean discoverable,
        Instant createdAt,
        Instant updatedAt) {}
