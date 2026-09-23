package com.wavelength.users;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.lang.Nullable;

import java.util.UUID;

@Schema(requiredProperties = {"id"})
public record PublicUser(
        UUID id,
        @Nullable String username,
        @Nullable String displayName,
        @Nullable String avatarUrl) {}
