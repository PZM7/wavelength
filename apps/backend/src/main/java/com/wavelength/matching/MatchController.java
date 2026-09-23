package com.wavelength.matching;

import com.wavelength.auth.CurrentUserProvider;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {
    private final CurrentUserProvider current;
    private final MatchService matches;

    public MatchController(CurrentUserProvider current, MatchService matches) {
        this.current = current;
        this.matches = matches;
    }

    @GetMapping
    public MatchPage list(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) @Nullable String cursor) {
        return matches.findTopMatches(current.get().getId(), limit, cursor);
    }
}
