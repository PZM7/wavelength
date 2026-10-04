package com.wavelength.social;

import java.util.List;
import org.springframework.lang.Nullable;

public record ChatMessagePage(List<ChatMessage> messages, @Nullable String nextCursor) {}
