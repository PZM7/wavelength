package com.wavelength.social;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wavelength.users.PublicUser;
import org.springframework.lang.Nullable;

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
        Instant updatedAt,
        @Nullable @JsonInclude(JsonInclude.Include.NON_NULL) PublicUser otherUser) {
    public ConnectionResponse(UUID id, UUID requesterId, UUID receiverId,
            ConnectionStatus status, Instant createdAt, Instant updatedAt) {
        this(id, requesterId, receiverId, status, createdAt, updatedAt, null);
    }
}
