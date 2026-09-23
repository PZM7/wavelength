package com.wavelength.matching;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.lang.Nullable;

@Schema(requiredProperties = {"label", "type"})
public record MatchReason(String type, String label, @Nullable Integer count) {}
