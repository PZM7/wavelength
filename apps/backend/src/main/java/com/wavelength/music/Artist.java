package com.wavelength.music;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "artists")
public class Artist {
    @Id private UUID id;

    private String name;

    private String normalizedName;

    private Instant createdAt;

    protected Artist() {}

    public Artist(String name, String normalizedName) {
        this(UUID.randomUUID(), name, normalizedName, Instant.now());
    }

    public Artist(UUID id, String name, String normalizedName, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.normalizedName = Objects.requireNonNull(normalizedName, "normalizedName");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
