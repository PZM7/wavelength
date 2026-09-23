package com.wavelength.social;

import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.UUID;

@Schema(requiredProperties = {"connections"})
public record ConnectionPage(List<ConnectionResponse> connections, @Nullable UUID nextCursor) {}
