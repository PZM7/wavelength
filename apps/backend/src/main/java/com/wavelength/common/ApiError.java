package com.wavelength.common;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp, String path) {
    public ApiError(String code, String message, String path) {
        this(code, message, Instant.now(), path);
    }
}
