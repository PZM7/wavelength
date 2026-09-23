package com.wavelength.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Opt-in only. Never cache JWTs, provider tokens or private profiles here.
@Component
public class CacheStore {
    private final StringRedisTemplate redis;

    public CacheStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Nullable
    public String get(String key) {
        return redis.opsForValue().get("wavelength:" + key);
    }

    public void put(String key, String value, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException();
        redis.opsForValue().set("wavelength:" + key, value, ttl);
    }

    public void evict(String key) {
        redis.delete("wavelength:" + key);
    }
}
