package com.wavelength.music.connect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavelength.common.ApiException;
import com.wavelength.music.ProviderArtistData;
import com.wavelength.music.ProviderTrackData;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SpotifyGateway {
    private static final String SCOPES = "user-read-private user-top-read";
    private final MusicConnectSettings settings;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final String accountsBase;
    private final String apiBase;

    public record Tokens(String accessToken, String refreshToken, int expiresIn) {}

    @Autowired
    public SpotifyGateway(MusicConnectSettings settings, ObjectMapper mapper) {
        this(settings, mapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                "https://accounts.spotify.com", "https://api.spotify.com");
    }

    SpotifyGateway(MusicConnectSettings settings, ObjectMapper mapper, HttpClient http,
            String accountsBase, String apiBase) {
        this.settings = settings;
        this.mapper = mapper;
        this.http = http;
        this.accountsBase = accountsBase;
        this.apiBase = apiBase;
    }

    public String authorizationUrl(String state, String verifier) {
        var challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier));
        var values = new LinkedHashMap<String, String>();
        values.put("response_type", "code");
        values.put("client_id", settings.spotifyClientId());
        values.put("redirect_uri", settings.spotifyRedirectUri());
        values.put("scope", SCOPES);
        values.put("state", state);
        values.put("code_challenge_method", "S256");
        values.put("code_challenge", challenge);
        return accountsBase + "/authorize?" + form(values);
    }

    public Tokens exchange(String code, String verifier) {
        var fields = new LinkedHashMap<String, String>();
        fields.put("grant_type", "authorization_code");
        fields.put("code", code);
        fields.put("redirect_uri", settings.spotifyRedirectUri());
        fields.put("client_id", settings.spotifyClientId());
        fields.put("code_verifier", verifier);
        return token(fields, true);
    }

    public Tokens refresh(String refreshToken) {
        return token(Map.of("grant_type", "refresh_token", "refresh_token", refreshToken,
                "client_id", settings.spotifyClientId()), false);
    }

    public String accountId(String accessToken) {
        var request = HttpRequest.newBuilder(URI.create(apiBase + "/v1/me"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json").GET().build();
        var response = send(request);
        if (response.statusCode() != 200) throw providerError();
        try {
            var id = mapper.readTree(response.body()).path("account_id").asText("");
            if (id.isBlank() || id.length() > 255) throw providerError();
            return id;
        } catch (IOException e) { throw providerError(); }
    }

    public List<ProviderArtistData> topArtists(String accessToken, String timeRange) {
        var items = topItems(accessToken, "artists", timeRange, 50);
        var artists = new ArrayList<ProviderArtistData>();
        for (var item : items) {
            var id = item.path("id").asText("");
            var name = item.path("name").asText("").trim();
            requireIdentity(id, name);
            var link = spotifyUrl(item.path("external_urls").path("spotify"), "artist");
            artists.add(new ProviderArtistData(id, name,
                    link == null ? null : imageUrl(item.path("images")), link));
        }
        return artists;
    }

    public List<ProviderTrackData> topTracks(String accessToken) {
        var items = topItems(accessToken, "tracks", "medium_term", 20);
        var tracks = new ArrayList<ProviderTrackData>();
        for (var item : items) {
            var id = item.path("id").asText("");
            var title = item.path("name").asText("").trim();
            requireIdentity(id, title);
            var artists = item.path("artists");
            if (!artists.isArray() || artists.isEmpty()) throw providerError();
            var artistId = artists.get(0).path("id").asText("");
            var artistName = artists.get(0).path("name").asText("").trim();
            requireIdentity(artistId, artistName);
            var link = spotifyUrl(item.path("external_urls").path("spotify"), "track");
            tracks.add(new ProviderTrackData(id, title,
                    new ProviderArtistData(artistId, artistName), null,
                    link == null ? null : imageUrl(item.path("album").path("images")), link));
        }
        return tracks;
    }

    private JsonNode topItems(String accessToken, String type, String timeRange, int limit) {
        if (!List.of("short_term", "medium_term", "long_term").contains(timeRange))
            throw new IllegalArgumentException("Unsupported Spotify time range");
        var request = HttpRequest.newBuilder(URI.create(apiBase + "/v1/me/top/" + type
                        + "?time_range=" + timeRange + "&limit=" + limit))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json").GET().build();
        var response = send(request);
        if (response.statusCode() == 401 || response.statusCode() == 403)
            throw new ApiException(HttpStatus.CONFLICT, "MUSIC_RECONNECT_REQUIRED",
                    "Reconnect Spotify to continue");
        if (response.statusCode() == 429)
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "MUSIC_PROVIDER_RATE_LIMITED",
                    "Spotify is limiting requests; try again later");
        if (response.statusCode() != 200) throw providerError();
        try {
            var items = mapper.readTree(response.body()).path("items");
            if (!items.isArray() || items.size() > limit) throw providerError();
            return items;
        } catch (IOException e) { throw providerError(); }
    }

    private static void requireIdentity(String id, String name) {
        if (id.isBlank() || id.length() > 255 || name.isBlank() || name.length() > 255
                || name.codePoints().anyMatch(Character::isISOControl)) throw providerError();
    }

    private static String imageUrl(JsonNode images) {
        if (!images.isArray()) return null;
        for (var image : images) {
            var url = image.path("url").asText("");
            if (url.length() > 2048) continue;
            try {
                var uri = URI.create(url);
                var host = uri.getHost();
                if ("https".equals(uri.getScheme()) && uri.getRawUserInfo() == null && host != null
                        && (host.equals("i.scdn.co") || host.endsWith(".spotifycdn.com"))) return url;
            } catch (IllegalArgumentException ignored) { /* Optional artwork may be absent. */ }
        }
        return null;
    }

    private static String spotifyUrl(JsonNode value, String type) {
        var url = value.asText("");
        if (url.length() > 2048) return null;
        try {
            var uri = URI.create(url);
            if ("https".equals(uri.getScheme()) && "open.spotify.com".equals(uri.getHost())
                    && uri.getRawUserInfo() == null && uri.getRawQuery() == null
                    && uri.getPath().startsWith("/" + type + "/")) return url;
        } catch (IllegalArgumentException ignored) { /* The link is optional. */ }
        return null;
    }

    private Tokens token(Map<String, String> fields, boolean initial) {
        var request = HttpRequest.newBuilder(URI.create(accountsBase + "/api/token"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form(fields))).build();
        var response = send(request);
        if (response.statusCode() != 200) {
            if (!initial && response.statusCode() == 400) {
                try {
                    if ("invalid_grant".equals(mapper.readTree(response.body()).path("error").asText()))
                        throw new ApiException(HttpStatus.CONFLICT, "MUSIC_RECONNECT_REQUIRED",
                                "Reconnect Spotify to continue");
                } catch (IOException ignored) { /* A malformed provider error is not user-actionable. */ }
            }
            throw providerError();
        }
        try {
            JsonNode body = mapper.readTree(response.body());
            var access = body.path("access_token").asText("");
            var refresh = body.path("refresh_token").asText("");
            var expires = body.path("expires_in").asInt(0);
            if (access.isBlank() || access.length() > 16_384 || (initial && refresh.isBlank())
                    || refresh.length() > 16_384 || expires <= 0 || expires > 86_400
                    || !"Bearer".equalsIgnoreCase(body.path("token_type").asText(""))) throw providerError();
            if (initial) {
                var scope = " " + body.path("scope").asText("") + " ";
                for (var required : SCOPES.split(" ")) if (!scope.contains(" " + required + " ")) throw providerError();
            }
            return new Tokens(access, refresh, expires);
        } catch (IOException e) { throw providerError(); }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try { return http.send(request, HttpResponse.BodyHandlers.ofString()); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw providerError();
        } catch (IOException e) { throw providerError(); }
    }

    private static byte[] sha256(String input) {
        try { return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.US_ASCII)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static String form(Map<String, String> values) {
        return values.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right).orElse("");
    }

    private static String encode(String input) { return URLEncoder.encode(input, StandardCharsets.UTF_8); }

    private static ApiException providerError() {
        return new ApiException(HttpStatus.BAD_GATEWAY, "MUSIC_PROVIDER_ERROR", "Music provider is unavailable");
    }
}
