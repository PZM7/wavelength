package com.wavelength.music;

import com.wavelength.auth.CurrentUserProvider;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ManualTasteController {
    private final CurrentUserProvider current;
    private final ManualTasteService taste;

    public ManualTasteController(CurrentUserProvider current, ManualTasteService taste) {
        this.current = current;
        this.taste = taste;
    }

    @GetMapping("/artists")
    public List<ArtistChoice> search(@RequestParam(defaultValue = "") String query) {
        return taste.search(query);
    }

    @GetMapping("/me/favorite-artists")
    public List<ArtistChoice> selected() {
        return taste.selected(current.get().getId());
    }

    @PutMapping("/me/favorite-artists")
    public List<ArtistChoice> replace(@RequestBody FavoriteArtistsRequest request) {
        return taste.replace(current.get().getId(), request);
    }
}
