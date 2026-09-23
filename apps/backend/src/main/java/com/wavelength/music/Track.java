package com.wavelength.music;

import jakarta.persistence.*;

import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tracks")
public class Track {
    @Id private UUID id;

    private String title;

    private UUID artistId;

    @Column(length = 12)
    @Nullable
    private String isrc;

    private Instant createdAt;

    protected Track() {}

    public Track(String title, UUID artistId) {
        this(UUID.randomUUID(), title, artistId, null, Instant.now());
    }

    public Track(UUID id, String title, UUID artistId, @Nullable String isrc, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.artistId = Objects.requireNonNull(artistId, "artistId");
        this.isrc = isrc;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public UUID getArtistId() {
        return artistId;
    }

    @Nullable
    public String getIsrc() {
        return isrc;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
