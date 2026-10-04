package com.wavelength.social;

import java.time.Instant;
import java.util.UUID;

public record ChatMessage(String id, UUID senderId, String body, Instant createdAt) {}
