package com.wavelength.social;

import com.wavelength.auth.CurrentUserProvider;
import com.wavelength.users.PublicUser;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class PublicUserController {
    private final CurrentUserProvider current;
    private final PrivacyService privacy;

    public PublicUserController(CurrentUserProvider current, PrivacyService privacy) {
        this.current = current;
        this.privacy = privacy;
    }

    @GetMapping("/{id}")
    public PublicUser profile(@PathVariable UUID id) {
        return privacy.profile(current.get().getId(), id);
    }

    @PostMapping("/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@PathVariable UUID id) {
        privacy.block(current.get().getId(), id);
    }

    @DeleteMapping("/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable UUID id) {
        privacy.unblock(current.get().getId(), id);
    }
}
