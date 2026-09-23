package com.wavelength.users;

import com.fasterxml.jackson.databind.JsonNode;
import com.wavelength.common.ApiException;
import com.wavelength.common.TextInput;

import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final Validator validator;
    private final boolean autoProvision;

    public UserService(
            UserRepository users,
            JdbcTemplate jdbc,
            Validator validator,
            @Value("${wavelength.auth.auto-provision}") boolean autoProvision) {
        this.users = users;
        this.jdbc = jdbc;
        this.validator = validator;
        this.autoProvision = autoProvision;
    }

    @Transactional
    public User resolve(String subject) {
        if (subject == null || TextInput.isBlank(subject) || subject.length() > 255)
            throw new IllegalArgumentException();
        var existing = users.findByExternalAuthId(subject);
        if (existing.isPresent()) return existing.get();
        if (!autoProvision) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "USER_NOT_PROVISIONED", "User provisioning is disabled");
        }
        // Atomic upsert preserves concurrent first-request provisioning.
        jdbc.update(
                "INSERT INTO users (external_auth_id) VALUES (?) ON CONFLICT (external_auth_id) DO"
                        + " NOTHING",
                subject);
        return users.findByExternalAuthId(subject).orElseThrow(ApiException::userNotFound);
    }

    @Transactional
    public UserProfile update(UUID id, JsonNode patch) throws URISyntaxException {
        if (!patch.isObject()) throw new IllegalArgumentException();
        var allowed =
                Set.of("username", "displayName", "avatarUrl", "city", "birthDate", "discoverable");
        patch.fieldNames()
                .forEachRemaining(
                        name -> {
                            if (!allowed.contains(name))
                                throw new IllegalArgumentException("Unknown or immutable field");
                        });
        var user = users.findById(id).orElseThrow(ApiException::userNotFound);
        String date =
                string(
                        patch,
                        "birthDate",
                        user.getBirthDate() == null ? null : user.getBirthDate().toString());
        var fields =
                new ProfileFields(
                        string(patch, "username", user.getUsername()),
                        string(patch, "displayName", user.getDisplayName()),
                        string(patch, "avatarUrl", user.getAvatarUrl()),
                        string(patch, "city", user.getCity()),
                        date == null ? null : LocalDate.parse(date));
        if (!validator.validate(fields).isEmpty()) throw new IllegalArgumentException();
        if (fields.avatarUrl() != null) {
            var uri = new URI(fields.avatarUrl());
            if (!"https".equals(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getHost().isBlank()
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        }
        user.setUsername(fields.username());
        user.setDisplayName(fields.displayName());
        user.setAvatarUrl(fields.avatarUrl());
        user.setCity(fields.city());
        user.setBirthDate(fields.birthDate());
        var discoverable = patch.get("discoverable");
        if (discoverable != null) {
            if (!discoverable.isBoolean()) throw new IllegalArgumentException();
            user.setDiscoverable(discoverable.asBoolean());
        }
        user.setUpdatedAt(Instant.now());
        return UserMapper.profile(users.saveAndFlush(user));
    }

    @Nullable
    private static String string(JsonNode patch, String name, @Nullable String previous) {
        var value = patch.get(name);
        if (value == null) return previous;
        if (!value.isNull() && !value.isTextual()) throw new IllegalArgumentException();
        return value.isNull() ? null : value.asText();
    }
}
