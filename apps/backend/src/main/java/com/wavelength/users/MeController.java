package com.wavelength.users;

import com.fasterxml.jackson.databind.JsonNode;
import com.wavelength.auth.CurrentUserProvider;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final CurrentUserProvider current;
    private final UserService users;

    public MeController(CurrentUserProvider current, UserService users) {
        this.current = current;
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "Current private profile; provisions on first valid JWT when enabled")
    public UserProfile me() {
        return UserMapper.profile(current.get());
    }

    @PatchMapping
    @Operation(
            summary =
                    "Partial profile update; omitted fields unchanged, null clears optional fields")
    public UserProfile update(@RequestBody JsonNode patch) throws URISyntaxException {
        return users.update(current.get().getId(), patch);
    }
}
