package com.wavelength.music.connect;

import com.wavelength.common.ApiException;
import com.wavelength.music.MusicProvider;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MusicConnectSettings {
    private final String spotifyClientId;
    private final String spotifyRedirectUri;
    private final String appleTeamId;
    private final String appleKeyId;
    private final String applePrivateKey;
    private final String publicBaseUrl;
    private final String webReturnUri;
    private final String nativeReturnUri;
    private final MusicTokenCipher cipher;

    public MusicConnectSettings(
            @Value("${wavelength.music.spotify-client-id:}") String spotifyClientId,
            @Value("${wavelength.music.spotify-redirect-uri:}") String spotifyRedirectUri,
            @Value("${wavelength.music.apple-team-id:}") String appleTeamId,
            @Value("${wavelength.music.apple-key-id:}") String appleKeyId,
            @Value("${wavelength.music.apple-private-key-base64:}") String applePrivateKey,
            @Value("${wavelength.music.public-base-url:}") String publicBaseUrl,
            @Value("${wavelength.music.web-return-uri:}") String webReturnUri,
            @Value("${wavelength.music.native-return-uri:wavelength://music/callback}") String nativeReturnUri,
            MusicTokenCipher cipher) {
        this.spotifyClientId = spotifyClientId.trim();
        this.spotifyRedirectUri = spotifyRedirectUri.trim();
        this.appleTeamId = appleTeamId.trim();
        this.appleKeyId = appleKeyId.trim();
        this.applePrivateKey = applePrivateKey.trim();
        this.publicBaseUrl = publicBaseUrl.trim();
        this.webReturnUri = webReturnUri.trim();
        this.nativeReturnUri = nativeReturnUri.trim();
        this.cipher = cipher;
    }

    public boolean enabled(MusicProvider provider) {
        if (!cipher.configured() || !validReturn(webReturnUri) || !validReturn(nativeReturnUri)) return false;
        return switch (provider) {
            case SPOTIFY -> !spotifyClientId.isBlank() && validHttp(spotifyRedirectUri);
            case APPLE_MUSIC -> !appleTeamId.isBlank() && !appleKeyId.isBlank()
                    && !applePrivateKey.isBlank() && validHttp(publicBaseUrl);
        };
    }

    public void require(MusicProvider provider) {
        if (!enabled(provider)) throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE, "MUSIC_NOT_CONFIGURED", "Music connection is not configured");
    }

    public String returnUri(String target) {
        return switch (target) {
            case "web" -> webReturnUri;
            case "native" -> nativeReturnUri;
            default -> throw new IllegalArgumentException("Unsupported return target");
        };
    }

    public String spotifyClientId() { return spotifyClientId; }
    public String spotifyRedirectUri() { return spotifyRedirectUri; }
    public String appleTeamId() { return appleTeamId; }
    public String appleKeyId() { return appleKeyId; }
    public String applePrivateKey() { return applePrivateKey; }
    public String publicBaseUrl() { return publicBaseUrl; }

    private static boolean validHttp(String value) {
        try {
            var uri = URI.create(value);
            if (uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawFragment() != null) return false;
            return "https".equals(uri.getScheme()) || ("http".equals(uri.getScheme())
                    && ("127.0.0.1".equals(uri.getHost()) || "[::1]".equals(uri.getHost())));
        } catch (IllegalArgumentException e) { return false; }
    }

    private static boolean validReturn(String value) {
        try {
            var uri = URI.create(value);
            if (uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getRawUserInfo() != null) return false;
            if (validHttp(value)) return true;
            if ("http".equals(uri.getScheme()) && "localhost".equals(uri.getHost())) return true;
            return "wavelength".equals(uri.getScheme()) && "music".equals(uri.getHost())
                    && "/callback".equals(uri.getPath());
        } catch (IllegalArgumentException e) { return false; }
    }
}
