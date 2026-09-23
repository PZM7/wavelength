package com.wavelength.config;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jwt.JwtException;

class SecurityConfigTest {
    @Test
    void acceptsSupabaseStyleEs256OnlyWithExpectedClaims() throws Exception {
        var key = new ECKeyGenerator(Curve.P_256).keyID("supabase-key").generate();
        var jwks = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (var stream = exchange.getResponseBody()) {
                stream.write(jwks);
            }
        });
        server.start();
        try {
            var issuer = "https://example.supabase.co/auth/v1";
            var decoder = new SecurityConfig(new ObjectMapper(), new MockEnvironment())
                    .jwtDecoder(issuer, "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks", "authenticated");
            assertEquals("user-123", decoder.decode(token(key, issuer, "authenticated")).getSubject());
            assertThrows(JwtException.class, () -> decoder.decode(token(key, issuer, "wrong")));
            assertThrows(JwtException.class, () -> decoder.decode(token(key, "https://wrong.invalid", "authenticated")));
        } finally {
            server.stop(0);
        }
    }

    private static String token(com.nimbusds.jose.jwk.ECKey key, String issuer, String audience)
            throws Exception {
        var signed = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(key.getKeyID()).build(),
                new JWTClaimsSet.Builder()
                        .subject("user-123")
                        .issuer(issuer)
                        .audience(audience)
                        .issueTime(Date.from(Instant.now()))
                        .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                        .build());
        signed.sign(new ECDSASigner(key));
        return signed.serialize();
    }
}
