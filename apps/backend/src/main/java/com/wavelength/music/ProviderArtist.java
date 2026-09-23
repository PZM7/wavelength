package com.wavelength.music;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "provider_artists")
public class ProviderArtist {
    @Id private UUID id;

    private UUID artistId;

    @Enumerated(EnumType.STRING)
    private MusicProvider provider;

    private String providerArtistId;

    protected ProviderArtist() {}

    public ProviderArtist(UUID artistId, MusicProvider provider, String providerArtistId) {
        this(UUID.randomUUID(), artistId, provider, providerArtistId);
    }

    public ProviderArtist(UUID id, UUID artistId, MusicProvider provider, String providerArtistId) {
        this.id = Objects.requireNonNull(id, "id");
        this.artistId = Objects.requireNonNull(artistId, "artistId");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.providerArtistId = Objects.requireNonNull(providerArtistId, "providerArtistId");
    }

    public UUID getId() {
        return id;
    }

    public UUID getArtistId() {
        return artistId;
    }

    public MusicProvider getProvider() {
        return provider;
    }

    public String getProviderArtistId() {
        return providerArtistId;
    }
}
