package com.wavelength.social;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "connections")
public class Connection {
    @Id private UUID id;

    private UUID requesterId;

    private UUID receiverId;

    @Enumerated(EnumType.STRING)
    private ConnectionStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    protected Connection() {}

    public Connection(UUID requesterId, UUID receiverId) {
        this(
                UUID.randomUUID(),
                requesterId,
                receiverId,
                ConnectionStatus.PENDING,
                Instant.now(),
                Instant.now());
    }

    public Connection(
            UUID id,
            UUID requesterId,
            UUID receiverId,
            ConnectionStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.requesterId = Objects.requireNonNull(requesterId, "requesterId");
        this.receiverId = Objects.requireNonNull(receiverId, "receiverId");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
