package com.wavelength.users;

import jakarta.persistence.*;

import org.springframework.lang.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private String externalAuthId;

    @Column(unique = true, length = 30)
    @Nullable
    private String username;

    @Column(length = 80)
    @Nullable
    private String displayName;

    @Column(length = 2048)
    @Nullable
    private String avatarUrl;

    @Column(length = 120)
    @Nullable
    private String city;

    @Nullable private LocalDate birthDate;

    private boolean discoverable;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected User() {}

    public User(String externalAuthId) {
        this(
                UUID.randomUUID(),
                externalAuthId,
                null,
                null,
                null,
                null,
                null,
                false,
                Instant.now(),
                Instant.now());
    }

    public User(
            UUID id,
            String externalAuthId,
            @Nullable String username,
            @Nullable String displayName,
            @Nullable String avatarUrl,
            @Nullable String city,
            @Nullable LocalDate birthDate,
            boolean discoverable,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.externalAuthId = Objects.requireNonNull(externalAuthId, "externalAuthId");
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.city = city;
        this.birthDate = birthDate;
        this.discoverable = discoverable;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public UUID getId() {
        return id;
    }

    public String getExternalAuthId() {
        return externalAuthId;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
    }

    @Nullable
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(@Nullable String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @Nullable
    public String getCity() {
        return city;
    }

    public void setCity(@Nullable String city) {
        this.city = city;
    }

    @Nullable
    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(@Nullable LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public boolean isDiscoverable() {
        return discoverable;
    }

    public void setDiscoverable(boolean discoverable) {
        this.discoverable = discoverable;
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
