package com.wavelength.music.connect;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppleMusicGatewayTest {
    @Test
    void signsDeveloperTokenAndVerifiesUserTokenWithAppleApi() throws Exception {
        var generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        var pair = generator.generateKeyPair();
        var settings = mock(MusicConnectSettings.class);
        when(settings.applePrivateKey()).thenReturn(Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
        when(settings.appleTeamId()).thenReturn("TEAM123456");
        when(settings.appleKeyId()).thenReturn("KEY1234567");
        when(settings.publicBaseUrl()).thenReturn("https://music.example.com");
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/me/storefront", exchange -> {
            assertEquals("user-token", exchange.getRequestHeaders().getFirst("Music-User-Token"));
            try {
                var jwt = SignedJWT.parse(exchange.getRequestHeaders().getFirst("Authorization").substring(7));
                assertTrue(jwt.verify(new ECDSAVerifier((java.security.interfaces.ECPublicKey) pair.getPublic())));
            } catch (Exception e) { throw new AssertionError(e); }
            var bytes = "{\"data\":[{\"id\":\"es\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        try {
            var gateway = new AppleMusicGateway(settings, new ObjectMapper(), HttpClient.newHttpClient(),
                    "http://127.0.0.1:" + server.getAddress().getPort());
            var web = SignedJWT.parse(gateway.developerToken(true));
            assertTrue(web.verify(new ECDSAVerifier((java.security.interfaces.ECPublicKey) pair.getPublic())));
            assertEquals("TEAM123456", web.getJWTClaimsSet().getIssuer());
            assertEquals(List.of("https://music.example.com"), web.getJWTClaimsSet().getClaim("origin"));
            assertTrue(gateway.verifyUserToken("user-token").startsWith("token-"));
        } finally { server.stop(0); }
    }
}
