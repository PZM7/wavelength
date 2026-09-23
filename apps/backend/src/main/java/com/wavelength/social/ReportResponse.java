package com.wavelength.social;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(requiredProperties = {"id"})
public record ReportResponse(UUID id) {}
