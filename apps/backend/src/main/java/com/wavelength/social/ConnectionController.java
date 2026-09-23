package com.wavelength.social;

import com.wavelength.auth.CurrentUserProvider;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionController {
    private final CurrentUserProvider current;
    private final ConnectionService service;

    public ConnectionController(CurrentUserProvider current, ConnectionService service) {
        this.current = current;
        this.service = service;
    }

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectionResponse request(@PathVariable UUID userId) {
        return service.request(current.get().getId(), userId);
    }

    @PostMapping("/{id}/accept")
    public ConnectionResponse accept(@PathVariable UUID id) {
        return service.respond(current.get().getId(), id, ConnectionStatus.ACCEPTED);
    }

    @PostMapping("/{id}/reject")
    public ConnectionResponse reject(@PathVariable UUID id) {
        return service.respond(current.get().getId(), id, ConnectionStatus.REJECTED);
    }

    @GetMapping
    public ConnectionPage list(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) @Nullable UUID cursor) {
        return service.list(current.get().getId(), limit, cursor);
    }
}
