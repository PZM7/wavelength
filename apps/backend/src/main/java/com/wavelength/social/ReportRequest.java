package com.wavelength.social;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.lang.Nullable;

import java.util.UUID;

public record ReportRequest(
        @NotNull UUID reportedUserId,
        @NotNull ReportReason reason,
        @Nullable @Size(max = 2000) String description) {}
