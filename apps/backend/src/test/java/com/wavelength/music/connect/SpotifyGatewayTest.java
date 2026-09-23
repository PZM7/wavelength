package com.wavelength.music.connect;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SpotifyGatewayTest {
    @Test
    void usesPkceAndExchangesAndRefreshesWithoutExposingTokensToClient() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/token", exchange -> {
            var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var initial = body.contains("grant_type=authorization_code");
            var revoked = body.contains("refresh_token=revoked");
            var response = revoked ? "{\"error\":\"invalid_grant\"}" : initial
                    ? "{\"access_token\":\"first\",\"refresh_token\":\"refresh\",\"expires_in\":3600,\"token_type\":\"Bearer\",\"scope\":\"user-read-private user-top-read user-read-recently-played\"}"
                    : "{\"access_token\":\"second\",\"expires_in\":3600,\"token_type\":\"Bearer\"}";
            if (initial) assertTrue(body.contains("code_verifier=verifier"));
            else assertTrue(body.contains("refresh_token=refresh") || revoked);
            var bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(revoked ? 400 : 200, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.createContext("/v1/me", exchange -> {
            assertEquals("Bearer first", exchange.getRequestHeaders().getFirst("Authorization"));
            var bytes = "{\"account_id\":\"stable-spotify-id\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        try {
            var settings = mock(MusicConnectSettings.class);
            when(settings.spotifyClientId()).thenReturn("public-client");
            when(settings.spotifyRedirectUri()).thenReturn("http://127.0.0.1:8080/api/v1/music/spotify/callback");
            var base = "http://127.0.0.1:" + server.getAddress().getPort();
            var gateway = new SpotifyGateway(settings, new ObjectMapper(), HttpClient.newHttpClient(), base, base);
            var url = URI.create(gateway.authorizationUrl("state", "verifier"));
            assertTrue(url.getRawQuery().contains("code_challenge_method=S256"));
            assertTrue(url.getRawQuery().contains("state=state"));
            var initial = gateway.exchange("code", "verifier");
            assertEquals("first", initial.accessToken());
            assertEquals("refresh", initial.refreshToken());
            assertEquals("stable-spotify-id", gateway.accountId(initial.accessToken()));
            var refreshed = gateway.refresh("refresh");
            assertEquals("second", refreshed.accessToken());
            assertEquals("", refreshed.refreshToken());
            assertEquals("MUSIC_RECONNECT_REQUIRED",
                    assertThrows(com.wavelength.common.ApiException.class, () -> gateway.refresh("revoked")).getCode());
        } finally { server.stop(0); }
    }
}
