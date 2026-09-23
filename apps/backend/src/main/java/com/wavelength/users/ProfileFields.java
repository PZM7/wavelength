package com.wavelength.users;

import jakarta.validation.constraints.*;

import org.springframework.lang.Nullable;

import java.time.LocalDate;

public record ProfileFields(
        @Nullable @Pattern(regexp = "[a-z0-9_]{3,30}") String username,
        @Nullable @Size(max = 80) String displayName,
        @Nullable @Size(max = 2048) String avatarUrl,
        @Nullable @Size(max = 120) String city,
        @Nullable @Past LocalDate birthDate) {}
