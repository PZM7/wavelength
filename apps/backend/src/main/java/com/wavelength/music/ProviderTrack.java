package com.wavelength.music;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "provider_tracks")
public class ProviderTrack {
    @Id private UUID id;

    private UUID trackId;

    @Enumerated(EnumType.STRING)
    private MusicProvider provider;

    private String providerTrackId;

    protected ProviderTrack() {}

    public ProviderTrack(UUID trackId, MusicProvider provider, String providerTrackId) {
        this(UUID.randomUUID(), trackId, provider, providerTrackId);
    }

    public ProviderTrack(UUID id, UUID trackId, MusicProvider provider, String providerTrackId) {
        this.id = Objects.requireNonNull(id, "id");
        this.trackId = Objects.requireNonNull(trackId, "trackId");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.providerTrackId = Objects.requireNonNull(providerTrackId, "providerTrackId");
    }

    public UUID getId() {
        return id;
    }

    public UUID getTrackId() {
        return trackId;
    }

    public MusicProvider getProvider() {
        return provider;
    }

    public String getProviderTrackId() {
        return providerTrackId;
    }
}
