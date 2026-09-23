package com.wavelength.social;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(
        requiredProperties = {
            "createdAt",
            "id",
            "receiverId",
            "requesterId",
            "status",
            "updatedAt"
        })
public record ConnectionResponse(
        UUID id,
        UUID requesterId,
        UUID receiverId,
        ConnectionStatus status,
        Instant createdAt,
        Instant updatedAt) {}
