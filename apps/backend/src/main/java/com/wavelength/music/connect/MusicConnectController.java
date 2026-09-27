package com.wavelength.music.connect;

import com.wavelength.auth.CurrentUserProvider;
import com.wavelength.music.MusicProvider;
import com.wavelength.music.SpotifyTasteSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/music-connections")
public class MusicConnectController {
    private final CurrentUserProvider current;
    private final MusicConnectSettings settings;
    private final MusicConnectService connect;
    private final MusicCredentialService credentials;
    private final SpotifyTasteSyncService tasteSync;

    public record Availability(boolean spotify, boolean appleMusic) {}

    public MusicConnectController(CurrentUserProvider current, MusicConnectSettings settings,
            MusicConnectService connect, MusicCredentialService credentials,
            SpotifyTasteSyncService tasteSync) {
        this.current = current;
        this.settings = settings;
        this.connect = connect;
        this.credentials = credentials;
        this.tasteSync = tasteSync;
    }

    @GetMapping("/availability")
    public Availability availability() {
        current.get();
        return new Availability(settings.enabled(MusicProvider.SPOTIFY), settings.enabled(MusicProvider.APPLE_MUSIC));
    }

    @PostMapping("/{provider}/start")
    public MusicConnectService.StartResponse start(@PathVariable MusicProvider provider, @RequestParam String target) {
        return connect.start(current.get().getId(), provider, target);
    }

    @PostMapping("/spotify/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refreshSpotify() { credentials.refreshSpotify(current.get().getId()); }

    @PostMapping("/spotify/sync")
    public SpotifyTasteSyncService.SyncResponse syncSpotify() {
        return tasteSync.sync(current.get().getId());
    }

    @DeleteMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(@PathVariable MusicProvider provider) {
        credentials.disconnect(current.get().getId(), provider);
    }
}
