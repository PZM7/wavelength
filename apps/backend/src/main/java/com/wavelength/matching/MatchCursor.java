package com.wavelength.matching;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public record MatchCursor(double score, UUID id) {
    public String encode() {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((score + "|" + id).getBytes(StandardCharsets.UTF_8));
    }

    public static MatchCursor decode(String value) {
        if (value.length() > 200) throw new IllegalArgumentException();
        var parts =
                new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
                        .split("\\|", -1);
        if (parts.length != 2) throw new IllegalArgumentException();
        double score = Double.parseDouble(parts[0]);
        if (!Double.isFinite(score) || score < 0.0 || score > 1.0)
            throw new IllegalArgumentException();
        return new MatchCursor(score, UUID.fromString(parts[1]));
    }
}
