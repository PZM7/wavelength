package com.wavelength.concerts;

import java.time.Instant;
import java.util.UUID;

public record Concert(
        UUID id,
        UUID artistId,
        String name,
        String venueName,
        String city,
        String country,
        Instant startsAt,
        String provider,
        String providerEventId) {}
