package com.wavelength.music.connect;

import com.wavelength.common.ApiException;
import com.wavelength.music.MusicProvider;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MusicConnectService {
    private final MusicConnectSettings settings;
    private final MusicConnectStateStore states;
    private final SpotifyGateway spotify;
    private final AppleMusicGateway apple;
    private final MusicCredentialService credentials;
    private final SecureRandom random = new SecureRandom();

    public record StartResponse(String authorizationUrl, String returnUri) {}
    public record ApplePage(String state, String developerToken) {}

    public MusicConnectService(MusicConnectSettings settings, MusicConnectStateStore states,
            SpotifyGateway spotify, AppleMusicGateway apple, MusicCredentialService credentials) {
        this.settings = settings;
        this.states = states;
        this.spotify = spotify;
        this.apple = apple;
        this.credentials = credentials;
    }

    public StartResponse start(UUID userId, MusicProvider provider, String target) {
        settings.require(provider);
        var returnUri = settings.returnUri(target);
        var verifier = provider == MusicProvider.SPOTIFY ? randomValue() : "";
        var state = states.create(new MusicConnectStateStore.Pending(userId, provider, returnUri, verifier));
        var url = provider == MusicProvider.SPOTIFY
                ? spotify.authorizationUrl(state, verifier)
                : settings.publicBaseUrl().replaceAll("/$", "") + "/music/apple/connect?state=" + state;
        return new StartResponse(url, returnUri);
    }

    public URI completeSpotify(String state, String code, String error) {
        var pending = consume(state, MusicProvider.SPOTIFY);
        if (error != null) return result(pending.returnUri(), "access_denied".equals(error) ? "cancelled" : "error", MusicProvider.SPOTIFY);
        if (code == null || code.isBlank() || code.length() > 4096)
            return result(pending.returnUri(), "error", MusicProvider.SPOTIFY);
        try {
            var tokens = spotify.exchange(code, pending.verifier());
            var id = spotify.accountId(tokens.accessToken());
            credentials.saveSpotify(pending.userId(), id, tokens);
            return result(pending.returnUri(), "connected", MusicProvider.SPOTIFY);
        } catch (RuntimeException e) {
            // The redirect is intentionally generic; OAuth codes and token responses
            // must never be reflected into the browser or its URL.
            return result(pending.returnUri(), "error", MusicProvider.SPOTIFY);
        }
    }

    public ApplePage applePage(String state) {
        settings.require(MusicProvider.APPLE_MUSIC);
        var pending = states.peek(state).orElseThrow(MusicConnectService::invalidState);
        if (pending.provider() != MusicProvider.APPLE_MUSIC) throw invalidState();
        return new ApplePage(state, apple.developerToken(true));
    }

    public URI completeApple(String state, String musicUserToken) {
        var preview = states.peek(state).orElseThrow(MusicConnectService::invalidState);
        if (preview.provider() != MusicProvider.APPLE_MUSIC) throw invalidState();
        var fingerprint = apple.verifyUserToken(musicUserToken);
        var pending = consume(state, MusicProvider.APPLE_MUSIC);
        credentials.saveApple(pending.userId(), fingerprint, musicUserToken);
        return result(pending.returnUri(), "connected", MusicProvider.APPLE_MUSIC);
    }

    private MusicConnectStateStore.Pending consume(String state, MusicProvider provider) {
        var pending = states.consume(state).orElseThrow(MusicConnectService::invalidState);
        if (pending.provider() != provider) throw invalidState();
        return pending;
    }

    private static ApiException invalidState() {
        return new ApiException(HttpStatus.BAD_REQUEST, "MUSIC_CONNECT_EXPIRED", "Music connection expired; start again");
    }

    private static URI result(String returnUri, String outcome, MusicProvider provider) {
        return UriComponentsBuilder.fromUriString(returnUri)
                .queryParam("music", outcome).queryParam("provider", provider.name()).build().toUri();
    }

    private String randomValue() {
        var bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
