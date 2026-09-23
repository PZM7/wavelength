package com.wavelength.music.connect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavelength.music.MusicProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class MusicConnectStateStore {
    private static final Duration LIFETIME = Duration.ofMinutes(10);
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final SecureRandom random = new SecureRandom();

    public record Pending(UUID userId, MusicProvider provider, String returnUri, String verifier) {}

    public MusicConnectStateStore(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public String create(Pending pending) {
        var bytes = new byte[32];
        random.nextBytes(bytes);
        var state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try {
            redis.opsForValue().set(key(state), mapper.writeValueAsString(pending), LIFETIME);
            return state;
        } catch (Exception e) { throw new IllegalStateException("Music connection state unavailable"); }
    }

    public Optional<Pending> peek(String state) { return read(state, false); }
    public Optional<Pending> consume(String state) { return read(state, true); }

    private Optional<Pending> read(String state, boolean consume) {
        if (state == null || !state.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
        try {
            var value = consume ? redis.opsForValue().getAndDelete(key(state)) : redis.opsForValue().get(key(state));
            return value == null ? Optional.empty() : Optional.of(mapper.readValue(value, Pending.class));
        } catch (Exception e) { throw new IllegalStateException("Music connection state unavailable"); }
    }

    private static String key(String state) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(state.getBytes(StandardCharsets.US_ASCII));
            return "wavelength:music-connect:" + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
