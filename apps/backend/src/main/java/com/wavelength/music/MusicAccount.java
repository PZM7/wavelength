package com.wavelength.music;

import jakarta.persistence.*;

import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "music_accounts")
public class MusicAccount {
    @Id private UUID id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private MusicProvider provider;

    private String providerUserId;

    @Column(columnDefinition = "text")
    @Nullable
    private String accessTokenEncrypted;

    @Column(columnDefinition = "text")
    @Nullable
    private String refreshTokenEncrypted;

    @Nullable private Instant tokenExpiresAt;

    private Instant createdAt;

    private Instant updatedAt;

    protected MusicAccount() {}

    public MusicAccount(UUID userId, MusicProvider provider, String providerUserId) {
        this(
                UUID.randomUUID(),
                userId,
                provider,
                providerUserId,
                null,
                null,
                null,
                Instant.now(),
                Instant.now());
    }

    public MusicAccount(
            UUID id,
            UUID userId,
            MusicProvider provider,
            String providerUserId,
            @Nullable String accessTokenEncrypted,
            @Nullable String refreshTokenEncrypted,
            @Nullable Instant tokenExpiresAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.providerUserId = Objects.requireNonNull(providerUserId, "providerUserId");
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.refreshTokenEncrypted = refreshTokenEncrypted;
        this.tokenExpiresAt = tokenExpiresAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public MusicProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    @Nullable
    public String getAccessTokenEncrypted() {
        return accessTokenEncrypted;
    }

    public void setAccessTokenEncrypted(@Nullable String accessTokenEncrypted) {
        this.accessTokenEncrypted = accessTokenEncrypted;
    }

    @Nullable
    public String getRefreshTokenEncrypted() {
        return refreshTokenEncrypted;
    }

    public void setRefreshTokenEncrypted(@Nullable String refreshTokenEncrypted) {
        this.refreshTokenEncrypted = refreshTokenEncrypted;
    }

    @Nullable
    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(@Nullable Instant tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
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
