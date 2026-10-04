package com.wavelength;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import com.wavelength.common.ApiException;
import com.wavelength.config.DevSeed;
import com.wavelength.matching.MatchService;
import com.wavelength.music.MusicAccount;
import com.wavelength.music.MusicAccountRepository;
import com.wavelength.music.MusicProvider;
import com.wavelength.music.ProviderArtistData;
import com.wavelength.music.ProviderTrackData;
import com.wavelength.music.SpotifyTasteSyncPersistence;
import com.wavelength.music.SpotifyTasteSyncService;
import com.wavelength.social.ConnectionService;
import com.wavelength.social.PrivacyService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegrationTest {
    private static final RSAKey KEY;
    private static final HttpServer JWKS_SERVER;

    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            JWKS_SERVER = HttpServer.create(new InetSocketAddress("0.0.0.0", 0), 0);
            JWKS_SERVER.createContext(
                    "/jwks",
                    exchange -> {
                        byte[] data =
                                new JWKSet(KEY.toPublicJWK())
                                        .toString()
                                        .getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, data.length);
                        try (var stream = exchange.getResponseBody()) {
                            stream.write(data);
                        }
                    });
            JWKS_SERVER.start();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("pgvector/pgvector:pg17")
                            .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("wavelength.auth.issuer", () -> "https://test-issuer.invalid/");
        registry.add(
                "wavelength.auth.jwk-set-uri",
                () -> "http://localhost:" + JWKS_SERVER.getAddress().getPort() + "/jwks");
        registry.add("wavelength.auth.audience", () -> "wavelength-api");
    }

    @AfterAll
    static void closeServer() {
        JWKS_SERVER.stop(0);
    }

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final MatchService matches;
    private final ConnectionService connections;
    private final PrivacyService privacy;
    private final UUID marc = DevSeed.userId(0);
    private final UUID lucia = DevSeed.userId(1);

    @Autowired private MusicAccountRepository musicAccounts;
    @Autowired private SpotifyTasteSyncPersistence spotifyTaste;

    @Autowired
    ApiIntegrationTest(
            MockMvc mvc,
            JdbcTemplate jdbc,
            ObjectMapper mapper,
            MatchService matches,
            ConnectionService connections,
            PrivacyService privacy) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.matches = matches;
        this.connections = connections;
        this.privacy = privacy;
    }

    private RequestPostProcessor asUser(String name) {
        return jwt().jwt(builder -> builder.subject("dev|" + name));
    }

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE users, artists CASCADE");
        new DevSeed(jdbc, "dev|marc").run(new DefaultApplicationArguments());
    }

    @Test
    void healthPublicPrivateRoutesRequireAuthAndProductionDocsDisabled() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void browserPreflightCanReachAuthenticatedApi() throws Exception {
        mvc.perform(
                        options("/api/v1/me")
                                .header("Origin", "http://localhost:8081")
                                .header("Access-Control-Request-Method", "GET")
                                .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"))
                .andExpect(header().string("Access-Control-Allow-Headers", "authorization"));

        mvc.perform(get("/api/v1/me").header("Origin", "http://localhost:8081"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }

    @Test
    void actualJwtSignatureIssuerAudienceExpiryAndSubjectAreValidated() throws Exception {
        mvc.perform(
                        get("/api/v1/me")
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + token(
                                                        "wavelength-api",
                                                        "https://test-issuer.invalid/",
                                                        300,
                                                        "dev|marc",
                                                        false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("marc"));
        for (String invalid :
                List.of(
                        token("wrong", "https://test-issuer.invalid/", 300, "dev|marc", false),
                        token(null, "https://test-issuer.invalid/", 300, "dev|marc", false),
                        token("wavelength-api", "https://other.invalid/", 300, "dev|marc", false),
                        token(
                                "wavelength-api",
                                "https://test-issuer.invalid/",
                                -120,
                                "dev|marc",
                                false),
                        token("wavelength-api", "https://test-issuer.invalid/", 300, "", false),
                        token(
                                "wavelength-api",
                                "https://test-issuer.invalid/",
                                300,
                                "\u00a0",
                                false),
                        token(
                                "wavelength-api",
                                "https://test-issuer.invalid/",
                                300,
                                "dev|marc",
                                true))) {
            mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void provisionsInternalUuidSafelyAndPatchRestrictionsApply() throws Exception {
        mvc.perform(get("/api/v1/me").with(asUser("new")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discoverable").value(false));
        mvc.perform(get("/api/v1/me").with(asUser("new"))).andExpect(status().isOk());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT count(*) FROM users WHERE external_auth_id = 'dev|new'",
                        Integer.class));
        mvc.perform(
                        patch("/api/v1/me")
                                .with(asUser("marc"))
                                .contentType("application/json")
                                .content(
                                        """
                                        {"city":"Madrid","displayName":null}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Madrid"))
                .andExpect(jsonPath("$.username").value("marc"));
        for (String body :
                List.of(
                        "{\"id\":\"x\"}",
                        "{\"externalAuthId\":\"x\"}",
                        "{\"username\":\"BAD!\"}",
                        "{\"birthDate\":\"not-date\"}",
                        "{\"discoverable\":null}")) {
            mvc.perform(
                            patch("/api/v1/me")
                                    .with(asUser("marc"))
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        patch("/api/v1/me")
                                .with(asUser("marc"))
                                .contentType("application/json")
                                .content("{\"username\":\"lucia\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rankedSqlMatchesMetricAndPaginatesWithoutDuplicates() throws Exception {
        new DevSeed(jdbc, "dev|marc").run(new DefaultApplicationArguments());
        assertEquals(28, jdbc.queryForObject("SELECT count(*) FROM music_account_artist_affinities", Integer.class));
        mvc.perform(get("/api/v1/me/music-accounts").with(asUser("marc")))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/me/music-dna").with(asUser("marc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topArtists.length()").value(7));
        mvc.perform(get("/api/v1/matches").with(asUser("marc")))
                .andExpect(jsonPath("$.matches.length()").value(3));
        var all = matches.findTopMatches(marc, 50);
        assertEquals(3, all.matches().size());
        assertEquals(lucia, all.matches().getFirst().user().id());
        all.matches()
                .forEach(
                        match ->
                                assertEquals(
                                        matches.calculateCompatibility(marc, match.user().id()),
                                        match.compatibility(),
                                        1e-12));
        var first = matches.findTopMatches(marc, 1);
        var second = matches.findTopMatches(marc, 1, first.nextCursor());
        assertNotEquals(
                first.matches().getFirst().user().id(), second.matches().getFirst().user().id());
        mvc.perform(get("/api/v1/matches?limit=51").with(asUser("marc")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/matches?cursor=bad").with(asUser("marc")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devSeedDoesNotReplaceAnExistingSpotifyAccount() {
        jdbc.update("DELETE FROM music_accounts WHERE user_id = ? AND provider = 'SPOTIFY'", marc);
        jdbc.update("INSERT INTO music_accounts (user_id, provider, provider_user_id) VALUES (?, 'SPOTIFY', 'real-account')", marc);
        new DevSeed(jdbc, "dev|marc").run(new DefaultApplicationArguments());
        assertEquals("real-account", jdbc.queryForObject(
                "SELECT provider_user_id FROM music_accounts WHERE user_id = ? AND provider = 'SPOTIFY'",
                String.class, marc));
        assertEquals(0, jdbc.queryForObject("""
SELECT count(*) FROM music_account_artist_affinities aa
JOIN music_accounts ma ON ma.id = aa.music_account_id WHERE ma.user_id = ?
""", Integer.class, marc));
    }

    @Test
    void spotifyArtistsWithSameNameKeepSeparateProviderIdentityAndArtwork() throws Exception {
        for (String subject : List.of("homonym-one", "homonym-two")) {
            mvc.perform(get("/api/v1/me").with(asUser(subject))).andExpect(status().isOk());
        }
        UUID first = jdbc.queryForObject("SELECT id FROM users WHERE external_auth_id = ?", UUID.class,
                "dev|homonym-one");
        UUID second = jdbc.queryForObject("SELECT id FROM users WHERE external_auth_id = ?", UUID.class,
                "dev|homonym-two");
        var firstAccount = musicAccounts.saveAndFlush(new MusicAccount(first, MusicProvider.SPOTIFY, "homonym-account-one"));
        var secondAccount = musicAccounts.saveAndFlush(new MusicAccount(second, MusicProvider.SPOTIFY, "homonym-account-two"));
        spotifyTaste.replace(first, firstAccount.getId(), List.of(new SpotifyTasteSyncService.ArtistScores(
                "homonym-spotify-one", "Same Name", 1, 0, 0,
                "https://example.com/one.jpg", null)), List.of());
        spotifyTaste.replace(second, secondAccount.getId(), List.of(new SpotifyTasteSyncService.ArtistScores(
                "homonym-spotify-two", "Same Name", 1, 0, 0,
                "https://example.com/two.jpg", null)), List.of());
        assertEquals(2, jdbc.queryForObject("SELECT count(DISTINCT artist_id) FROM provider_artists WHERE provider_artist_id LIKE 'homonym-spotify-%'", Integer.class));
        mvc.perform(get("/api/v1/me/music-dna").with(asUser("homonym-one")))
                .andExpect(jsonPath("$.topArtists[0].imageUrl").value("https://example.com/one.jpg"));
        mvc.perform(get("/api/v1/me/music-dna").with(asUser("homonym-two")))
                .andExpect(jsonPath("$.topArtists[0].imageUrl").value("https://example.com/two.jpg"));
        assertEquals(0, matches.calculateCompatibility(first, second));
    }

    @Test
    void manualArtistEndpointIsGoneAndOldSelectionsDoNotAffectTaste() throws Exception {
        mvc.perform(get("/api/v1/me").with(asUser("new"))).andExpect(status().isOk());
        UUID userId = jdbc.queryForObject("SELECT id FROM users WHERE external_auth_id = ?", UUID.class,
                "dev|new");
        jdbc.update("INSERT INTO user_manual_artist_preferences (user_id, artist_id) VALUES (?, ?)",
                userId, DevSeed.artistId(0));
        jdbc.update("""
                INSERT INTO user_artist_affinities
                    (user_id, artist_id, short_term_score, medium_term_score, long_term_score)
                VALUES (?, ?, 1, 1, 1)
                """, userId, DevSeed.artistId(1));
        mvc.perform(put("/api/v1/me/favorite-artists").with(asUser("new"))
                .contentType("application/json").content("{\"names\":[\"Sampha\"]}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/me/music-dna").with(asUser("new")))
                .andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.topArtists.length()").value(0));
    }

    @Test
    void spotifySyncReplacesOnlyImportedTasteAndDisconnectRemovesIt() throws Exception {
        mvc.perform(get("/api/v1/me").with(asUser("spotify-sync"))).andExpect(status().isOk());
        UUID userId = jdbc.queryForObject("SELECT id FROM users WHERE external_auth_id = ?", UUID.class,
                "dev|spotify-sync");
        var account = musicAccounts.saveAndFlush(new MusicAccount(userId, MusicProvider.SPOTIFY,
                "spotify-sync-user"));
        spotifyTaste.replace(userId, account.getId(), List.of(
                new SpotifyTasteSyncService.ArtistScores("spotify-sampha", "Sampha", 1, 0.8, 0.6,
                        "https://i.scdn.co/image/sampha", "https://open.spotify.com/artist/spotify-sampha"),
                new SpotifyTasteSyncService.ArtistScores("spotify-bjork", "Björk", 0.9, 0, 0)),
                List.of(new ProviderTrackData("spotify-track-1", "Spirit 2.0",
                        new ProviderArtistData("spotify-sampha", "Sampha"), null,
                        "https://i.scdn.co/image/spirit", "https://open.spotify.com/track/spotify-track-1")));
        mvc.perform(get("/api/v1/me/music-dna").with(asUser("spotify-sync")))
                .andExpect(jsonPath("$.topArtists.length()").value(2))
                .andExpect(jsonPath("$.topArtists[0].imageUrl").value("https://i.scdn.co/image/sampha"));
        mvc.perform(get("/api/v1/me/top-tracks").with(asUser("spotify-sync")))
                .andExpect(jsonPath("$[0].title").value("Spirit 2.0"))
                .andExpect(jsonPath("$[0].imageUrl").value("https://i.scdn.co/image/spirit"));
        mvc.perform(get("/api/v1/matches").with(asUser("spotify-sync")))
                .andExpect(jsonPath("$.matches.length()").value(0));

        spotifyTaste.replace(userId, account.getId(), List.of(
                new SpotifyTasteSyncService.ArtistScores("spotify-massive", "Massive Attack", 1, 0.5, 0)),
                List.of());
        mvc.perform(get("/api/v1/me/music-dna").with(asUser("spotify-sync")))
                .andExpect(jsonPath("$.topArtists.length()").value(1))
                .andExpect(jsonPath("$.topArtists[0].name").value("Massive Attack"));
        mvc.perform(get("/api/v1/me/top-tracks").with(asUser("spotify-sync")))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/v1/me/music-connections/SPOTIFY").with(asUser("spotify-sync")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/music-dna").with(asUser("spotify-sync")))
                .andExpect(jsonPath("$.topArtists.length()").value(0));
    }

    @Test
    void connectionReceiverOwnsTransitionsAndReverseDuplicatesAreRefused() throws Exception {
        mvc.perform(get("/api/v1/connections/with/" + lucia).with(asUser("marc")))
                .andExpect(status().isNoContent());
        var response =
                mvc.perform(post("/api/v1/connections/" + lucia).with(asUser("marc")))
                        .andExpect(status().isCreated())
                        .andReturn();
        var id = mapper.readTree(response.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(get("/api/v1/connections/with/" + lucia).with(asUser("marc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requesterId").value(marc.toString()));
        mvc.perform(get("/api/v1/connections/with/" + marc).with(asUser("lucia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requesterId").value(marc.toString()));
        mvc.perform(get("/api/v1/connections").with(asUser("lucia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connections[0].otherUser.id").value(marc.toString()))
                .andExpect(jsonPath("$.connections[0].otherUser.displayName").value("Marc"));
        mvc.perform(post("/api/v1/connections/" + marc).with(asUser("lucia")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/connections/" + id + "/accept").with(asUser("marc")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/connections/" + id + "/accept").with(asUser("alex")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/connections/" + id + "/accept").with(asUser("lucia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(get("/api/v1/connections/with/" + marc).with(asUser("lucia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(post("/api/v1/connections/" + id + "/reject").with(asUser("lucia")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/connections/" + marc).with(asUser("marc")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatRequiresAcceptedConnectionAndBlockRevokesBothParticipants() throws Exception {
        var connection = connections.request(marc, lucia);
        String path = "/api/v1/conversations/" + connection.id();
        mvc.perform(get(path).with(asUser("marc"))).andExpect(status().isNotFound());
        mvc.perform(get(path + "/messages").with(asUser("marc"))).andExpect(status().isNotFound());
        mvc.perform(post(path + "/messages").with(asUser("marc"))
                .contentType("application/json").content("{\"body\":\"Hola\",\"clientMessageId\":\"first\"}"))
                .andExpect(status().isNotFound());
        connections.respond(lucia, connection.id(), com.wavelength.social.ConnectionStatus.ACCEPTED);
        jdbc.update("UPDATE users SET discoverable = false WHERE id = ?", lucia);
        mvc.perform(get(path).with(asUser("marc")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.otherUser.id").value(lucia.toString()));
        mvc.perform(post(path + "/messages").with(asUser("marc"))
                .contentType("application/json").content("{\"body\":\" Hola Lucía \",\"clientMessageId\":\"first\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Hola Lucía"))
                .andExpect(jsonPath("$.senderId").value(marc.toString()));
        mvc.perform(get(path + "/messages").with(asUser("lucia")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.messages[0].body").value("Hola Lucía"));
        mvc.perform(get(path).with(asUser("alex"))).andExpect(status().isNotFound());
        mvc.perform(get(path + "/messages").with(asUser("alex"))).andExpect(status().isNotFound());
        mvc.perform(post(path + "/messages").with(asUser("alex"))
                .contentType("application/json").content("{\"body\":\"Hola\",\"clientMessageId\":\"third-party\"}"))
                .andExpect(status().isNotFound());
        privacy.block(lucia, marc);
        for (String subject : List.of("marc", "lucia")) {
            mvc.perform(get(path + "/messages").with(asUser(subject))).andExpect(status().isNotFound());
            mvc.perform(post(path + "/messages").with(asUser(subject))
                    .contentType("application/json").content("{\"body\":\"Otro\",\"clientMessageId\":\"after-block\"}"))
                    .andExpect(status().isNotFound());
        }
        privacy.unblock(lucia, marc);
        mvc.perform(get(path + "/messages").with(asUser("marc"))).andExpect(status().isNotFound());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM chat_messages", Integer.class));
    }

    @Test
    void chatPersistsMessagesPaginatesAndDeduplicatesRetries() throws Exception {
        var connection = connections.request(marc, lucia);
        connections.respond(lucia, connection.id(), com.wavelength.social.ConnectionStatus.ACCEPTED);
        String path = "/api/v1/conversations/" + connection.id() + "/messages";
        String firstBody = "{\"body\":\"Primero\",\"clientMessageId\":\"one\"}";
        var first = mvc.perform(post(path).with(asUser("marc")).contentType("application/json").content(firstBody))
                .andExpect(status().isCreated()).andReturn();
        String firstId = mapper.readTree(first.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post(path).with(asUser("marc")).contentType("application/json").content(firstBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(firstId));
        mvc.perform(post(path).with(asUser("marc")).contentType("application/json")
                .content("{\"body\":\"Contenido distinto\",\"clientMessageId\":\"one\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post(path).with(asUser("lucia")).contentType("application/json")
                .content("{\"body\":\"Segundo\",\"clientMessageId\":\"one\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.senderId").value(lucia.toString()));
        mvc.perform(post(path).with(asUser("marc")).contentType("application/json")
                .content("{\"body\":\"Tercero\",\"clientMessageId\":\"three\"}"))
                .andExpect(status().isCreated());
        var latest = mvc.perform(get(path + "?limit=2").with(asUser("lucia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].body").value("Segundo"))
                .andExpect(jsonPath("$.messages[1].body").value("Tercero")).andReturn();
        String cursor = mapper.readTree(latest.getResponse().getContentAsString()).get("nextCursor").asText();
        mvc.perform(get(path + "?limit=2&before=" + cursor).with(asUser("lucia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].id").value(firstId))
                .andExpect(jsonPath("$.nextCursor").isEmpty());
        for (String invalidBody : List.of(" ", "x".repeat(2001))) {
            mvc.perform(post(path).with(asUser("marc")).contentType("application/json")
                    .content(mapper.writeValueAsString(Map.of("body", invalidBody, "clientMessageId", "invalid"))))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get(path + "?before=0").with(asUser("marc"))).andExpect(status().isBadRequest());
        mvc.perform(get(path + "?limit=101").with(asUser("marc"))).andExpect(status().isBadRequest());
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM chat_messages", Integer.class));
    }

    @Test
    void connectionListAppliesProfileVisibilityBeforePagination() throws Exception {
        UUID alex = DevSeed.userId(2);
        UUID nora = DevSeed.userId(3);
        var rejected = connections.request(marc, lucia);
        connections.respond(lucia, rejected.id(), com.wavelength.social.ConnectionStatus.REJECTED);
        connections.request(marc, alex);
        var accepted = connections.request(marc, nora);
        connections.respond(nora, accepted.id(), com.wavelength.social.ConnectionStatus.ACCEPTED);
        // Put hidden rows first so filtering after LIMIT would produce an empty page.
        jdbc.update("UPDATE connections SET id = ? WHERE receiver_id = ?", UUID.fromString("00000000-0000-0000-0000-000000000001"), lucia);
        jdbc.update("UPDATE connections SET id = ? WHERE receiver_id = ?", UUID.fromString("00000000-0000-0000-0000-000000000002"), alex);
        jdbc.update("UPDATE connections SET id = ? WHERE receiver_id = ?", UUID.fromString("00000000-0000-0000-0000-000000000003"), nora);
        for (UUID target : List.of(lucia, alex, nora)) {
            jdbc.update("UPDATE users SET discoverable = false WHERE id = ?", target);
        }
        jdbc.update("UPDATE users SET username = 'hidden_current', display_name = 'Hidden current name', avatar_url = 'https://example.com/hidden-current.jpg' WHERE id = ?", lucia);
        mvc.perform(get("/api/v1/users/" + lucia).with(asUser("marc")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/users/" + alex).with(asUser("marc")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/users/" + nora).with(asUser("marc")))
                .andExpect(status().isOk());
        String body = mvc.perform(get("/api/v1/connections?limit=1").with(asUser("marc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connections.length()").value(1))
                .andExpect(jsonPath("$.connections[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.connections[0].otherUser.id").value(nora.toString()))
                .andExpect(jsonPath("$.nextCursor").isEmpty())
                .andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("hidden_current"));
        assertFalse(body.contains("Hidden current name"));
        assertFalse(body.contains("hidden-current.jpg"));
    }

    @Test
    void blockingHidesBothDirectionsAndTerminatesExistingConnections() throws Exception {
        connections.request(marc, lucia);
        mvc.perform(post("/api/v1/users/" + lucia + "/block").with(asUser("marc")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/blocked").with(asUser("marc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(lucia.toString()));
        mvc.perform(get("/api/v1/users/blocked").with(asUser("lucia")))
                .andExpect(jsonPath("$.length()").value(0));
        assertFalse(
                matches.findTopMatches(marc, 50).matches().stream()
                        .anyMatch(match -> match.user().id().equals(lucia)));
        assertFalse(
                matches.findTopMatches(lucia, 50).matches().stream()
                        .anyMatch(match -> match.user().id().equals(marc)));
        mvc.perform(get("/api/v1/users/" + marc).with(asUser("lucia")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/connections/" + marc).with(asUser("lucia")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/connections").with(asUser("marc")))
                .andExpect(jsonPath("$.connections").isEmpty());
        privacy.block(lucia, marc);
        privacy.unblock(marc, lucia);
        assertTrue(privacy.blocked(marc, lucia));
        assertEquals(
                "REJECTED", jdbc.queryForObject("SELECT status FROM connections", String.class));
    }

    @Test
    void hiddenProfilesAndTokensDoNotLeak() throws Exception {
        jdbc.update("UPDATE users SET discoverable = false WHERE id = ?", lucia);
        mvc.perform(get("/api/v1/users/" + lucia).with(asUser("marc")))
                .andExpect(status().isNotFound());
        assertFalse(
                matches.findTopMatches(marc, 50).matches().stream()
                        .anyMatch(match -> match.user().id().equals(lucia)));
        jdbc.update(
                """
UPDATE music_accounts SET access_token_encrypted = 'encrypted-test-fixture',
    provider_user_id = 'provider-user' WHERE user_id = ? AND provider = 'SPOTIFY'
""",
                marc);
        String body =
                mvc.perform(get("/api/v1/me/music-accounts").with(asUser("marc")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertFalse(body.contains("encrypted"));
        assertFalse(body.contains("provider-user"));
        mvc.perform(get("/api/v1/users/" + marc).with(asUser("alex")))
                .andExpect(jsonPath("$.birthDate").doesNotExist())
                .andExpect(jsonPath("$.externalAuthId").doesNotExist());
    }

    @Test
    void databaseConstraintsEnforceBoundedScoresAndUnorderedUniquePairs() {
        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        jdbc.update(
                                "UPDATE music_account_artist_affinities SET short_term_score = 1.1 WHERE"
                                    + " music_account_id = (SELECT id FROM music_accounts WHERE user_id = ? AND provider = 'SPOTIFY')",
                                marc));
        jdbc.update(
                "INSERT INTO connections (requester_id, receiver_id, status) VALUES (?, ?,"
                    + " 'PENDING')",
                marc,
                lucia);
        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        jdbc.update(
                                "INSERT INTO connections (requester_id, receiver_id, status) VALUES"
                                    + " (?, ?, 'PENDING')",
                                lucia,
                                marc));
        assertEquals(
                "vector",
                jdbc.queryForObject(
                        "SELECT extname FROM pg_extension WHERE extname = 'vector'", String.class));
    }

    @Test
    void reportsAreValidatedPersistedAndCanFollowABlock() throws Exception {
        privacy.block(marc, lucia);
        mvc.perform(
                        post("/api/v1/reports")
                                .with(asUser("marc"))
                                .contentType("application/json")
                                .content(
                                        "{\"reportedUserId\":\""
                                                + lucia
                                                + "\",\"reason\":\"HARASSMENT\",\"description\":\"Test"
                                                + " report\"}"))
                .andExpect(status().isCreated());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM reports", Integer.class));
    }

    @Test
    void concurrentBlockAndRequestCannotLeaveAnActiveConnection() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var request =
                    executor.submit(
                            () -> {
                                start.await();
                                try {
                                    connections.request(marc, lucia);
                                } catch (ApiException e) {
                                    assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
                                }
                                return null;
                            });
            var block =
                    executor.submit(
                            () -> {
                                start.await();
                                privacy.block(lucia, marc);
                                return null;
                            });
            start.countDown();
            request.get(15, TimeUnit.SECONDS);
            block.get(15, TimeUnit.SECONDS);
            assertTrue(privacy.blocked(marc, lucia));
            assertEquals(
                    0,
                    jdbc.queryForObject(
                            "SELECT count(*) FROM connections WHERE status <> 'REJECTED'",
                            Integer.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void requiredReportFieldsStillReturn400WithoutKotlinJacksonModule() throws Exception {
        for (String body :
                List.of(
                        "{}",
                        "{\"reportedUserId\":null,\"reason\":\"SPAM\"}",
                        "{\"reportedUserId\":\"" + lucia + "\"}",
                        "{\"reportedUserId\":\"" + lucia + "\",\"reason\":null}")) {
            mvc.perform(
                            post("/api/v1/reports")
                                    .with(asUser("marc"))
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mvc.perform(
                        post("/api/v1/reports")
                                .with(asUser("marc"))
                                .contentType("application/json")
                                .content(
                                        "{\"reportedUserId\":\""
                                                + lucia
                                                + "\",\"reason\":\"SPAM\"}"))
                .andExpect(status().isCreated());
    }

    private static String token(
            String audience, String issuer, long expiry, String subject, boolean wrongKey)
            throws Exception {
        var signed =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),
                        new JWTClaimsSet.Builder()
                                .subject(subject)
                                .issuer(issuer)
                                .audience(audience)
                                .issueTime(Date.from(Instant.now()))
                                .expirationTime(Date.from(Instant.now().plusSeconds(expiry)))
                                .build());
        signed.sign(new RSASSASigner(wrongKey ? new RSAKeyGenerator(2048).generate() : KEY));
        return signed.serialize();
    }
}
