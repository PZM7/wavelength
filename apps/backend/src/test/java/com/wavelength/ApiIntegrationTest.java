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
    void connectionReceiverOwnsTransitionsAndReverseDuplicatesAreRefused() throws Exception {
        var response =
                mvc.perform(post("/api/v1/connections/" + lucia).with(asUser("marc")))
                        .andExpect(status().isCreated())
                        .andReturn();
        var id = mapper.readTree(response.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/api/v1/connections/" + marc).with(asUser("lucia")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/connections/" + id + "/accept").with(asUser("marc")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/connections/" + id + "/accept").with(asUser("alex")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/connections/" + id + "/accept").with(asUser("lucia")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(post("/api/v1/connections/" + id + "/reject").with(asUser("lucia")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/connections/" + marc).with(asUser("marc")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blockingHidesBothDirectionsAndTerminatesExistingConnections() throws Exception {
        connections.request(marc, lucia);
        mvc.perform(post("/api/v1/users/" + lucia + "/block").with(asUser("marc")))
                .andExpect(status().isNoContent());
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
INSERT INTO music_accounts (user_id, provider, provider_user_id, access_token_encrypted)
VALUES (?, 'SPOTIFY', 'provider-user', 'encrypted-test-fixture')
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
                                "UPDATE user_artist_affinities SET short_term_score = 1.1 WHERE"
                                    + " user_id = ?",
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
