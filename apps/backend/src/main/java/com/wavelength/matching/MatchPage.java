package com.wavelength.matching;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.lang.Nullable;

import java.util.List;

@Schema(requiredProperties = {"matches"})
public record MatchPage(List<MatchResult> matches, @Nullable String nextCursor) {}
