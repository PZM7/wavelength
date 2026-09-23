package com.wavelength.music.connect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.wavelength.common.ApiException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AppleMusicGateway {
    private final MusicConnectSettings settings;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final String apiBase;

    @Autowired
    public AppleMusicGateway(MusicConnectSettings settings, ObjectMapper mapper) {
        this(settings, mapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                "https://api.music.apple.com");
    }

    AppleMusicGateway(MusicConnectSettings settings, ObjectMapper mapper, HttpClient http, String apiBase) {
        this.settings = settings;
        this.mapper = mapper;
        this.http = http;
        this.apiBase = apiBase;
    }

    public String developerToken(boolean forWeb) {
        try {
            var keyBytes = Base64.getDecoder().decode(settings.applePrivateKey());
            var privateKey = (ECPrivateKey) KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            var now = Instant.now();
            var claims = new JWTClaimsSet.Builder().issuer(settings.appleTeamId())
                    .issueTime(Date.from(now)).expirationTime(Date.from(now.plus(Duration.ofHours(1))));
            if (forWeb) {
                var origin = URI.create(settings.publicBaseUrl());
                claims.claim("origin", List.of(origin.getScheme() + "://" + origin.getAuthority()));
            }
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(settings.appleKeyId()).build(), claims.build());
            jwt.sign(new ECDSASigner(privateKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MUSIC_NOT_CONFIGURED",
                    "Apple Music configuration is invalid");
        }
    }

    public String verifyUserToken(String musicUserToken) {
        if (musicUserToken == null || musicUserToken.isBlank() || musicUserToken.length() > 16_384)
            throw new IllegalArgumentException("Invalid Apple Music token");
        var request = HttpRequest.newBuilder(URI.create(apiBase + "/v1/me/storefront"))
                .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + developerToken(false))
                .header("Music-User-Token", musicUserToken).header("Accept", "application/json")
                .GET().build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403)
                throw new ApiException(HttpStatus.BAD_REQUEST, "MUSIC_AUTH_INVALID", "Apple Music authorization is invalid");
            if (response.statusCode() != 200)
                throw new ApiException(HttpStatus.BAD_GATEWAY, "MUSIC_PROVIDER_ERROR", "Apple Music is unavailable");
            var data = mapper.readTree(response.body()).path("data");
            if (!data.isArray() || data.isEmpty())
                throw new ApiException(HttpStatus.BAD_GATEWAY, "MUSIC_PROVIDER_ERROR", "Apple Music is unavailable");
            // MusicKit does not expose a stable account identifier. A token fingerprint
            // prevents linking the same issued token to two Wavelength accounts.
            return "token-" + Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(musicUserToken));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "MUSIC_PROVIDER_ERROR", "Apple Music is unavailable");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "MUSIC_PROVIDER_ERROR", "Apple Music is unavailable");
        }
    }

    private static byte[] sha256(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
