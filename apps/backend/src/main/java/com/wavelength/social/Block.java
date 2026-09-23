package com.wavelength.social;

import java.time.Instant;
import java.util.UUID;

public record Block(UUID blockerId, UUID blockedId, Instant createdAt) {}
