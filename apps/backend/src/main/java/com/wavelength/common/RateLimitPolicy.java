package com.wavelength.common;

// Integration point only; no active limiter. Gateway/filter must use the error envelope +
// Retry-After.
public record RateLimitPolicy(int requests, long windowSeconds) {
    public RateLimitPolicy {
        if (requests <= 0 || windowSeconds <= 0) throw new IllegalArgumentException();
    }
}
