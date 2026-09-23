package com.wavelength.social;

import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.UUID;

public record Report(
        UUID id,
        UUID reporterId,
        UUID reportedUserId,
        ReportReason reason,
        @Nullable String description,
        Instant createdAt) {}
