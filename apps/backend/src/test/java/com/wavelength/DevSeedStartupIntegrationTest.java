package com.wavelength;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wavelength.config.DevSeed;
import com.wavelength.matching.MatchService;
import com.wavelength.music.MusicDnaService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest(properties = {
        "spring.profiles.active=dev",
        "DEV_SEED_ENABLED=true",
        "wavelength.auth.issuer=https://test-issuer.invalid/",
        "wavelength.auth.jwk-set-uri=https://test-issuer.invalid/jwks",
        "wavelength.auth.audience=wavelength-api"
})
@Testcontainers
class DevSeedStartupIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg17")
                    .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MatchService matches;
    @Autowired MusicDnaService dna;

    @Test
    void devStartupSeedsTheSameTasteUsedByDnaAndMatching() {
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM users", Integer.class));
        assertEquals(28, jdbc.queryForObject(
                "SELECT count(*) FROM music_account_artist_affinities", Integer.class));
        assertEquals(7, dna.get(DevSeed.userId(0)).topArtists().size());
        assertEquals(3, matches.findTopMatches(DevSeed.userId(0), 20).matches().size());
    }
}
