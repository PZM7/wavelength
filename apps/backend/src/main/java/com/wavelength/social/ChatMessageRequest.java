package com.wavelength.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank @Size(max = 2000) String body,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String clientMessageId) {}
