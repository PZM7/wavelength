package com.wavelength.music;

import java.time.Instant;

public record ListeningData(ProviderTrackData track, Instant playedAt) {}
