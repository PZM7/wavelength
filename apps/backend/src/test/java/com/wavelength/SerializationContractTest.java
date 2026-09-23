package com.wavelength;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavelength.common.ApiError;
import com.wavelength.matching.*;
import com.wavelength.music.*;
import com.wavelength.social.*;
import com.wavelength.users.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonTest
class SerializationContractTest {
    private final ObjectMapper mapper;

    @Autowired
    SerializationContractTest(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Test
    void recordsMatchJsonProducedByOriginalKotlinJar() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000101");
        Instant time = Instant.parse("2026-01-02T03:04:05Z");
        var user = new PublicUser(id, "marc", "Marc", null);
        var snapshots = new LinkedHashMap<String, Object>();
        snapshots.put(
                "profile",
                new UserProfile(
                        id,
                        "marc",
                        null,
                        null,
                        null,
                        LocalDate.parse("1998-06-15"),
                        false,
                        time,
                        time));
        snapshots.put("publicUser", user);
        snapshots.put(
                "musicAccount", new MusicAccountResponse(id, MusicProvider.APPLE_MUSIC, time));
        snapshots.put(
                "dna",
                new MusicDna(
                        "ARTIST_SIGNALS_ONLY",
                        null,
                        Map.of(),
                        List.of(new ArtistWeight(id, "Sampha", 0.5))));
        snapshots.put(
                "matches",
                new MatchPage(
                        List.of(
                                new MatchResult(
                                        user,
                                        0.5,
                                        List.of(
                                                new MatchReason(
                                                        "SHARED_ARTISTS", "1 shared artists", 1)))),
                        null));
        snapshots.put(
                "connections",
                new ConnectionPage(
                        List.of(
                                new ConnectionResponse(
                                        id,
                                        id,
                                        UUID.fromString("00000000-0000-0000-0000-000000000102"),
                                        ConnectionStatus.PENDING,
                                        time,
                                        time)),
                        null));
        snapshots.put("report", new ReportResponse(id));
        snapshots.put(
                "error",
                new ApiError("USER_NOT_FOUND", "User not found", time, "/api/v1/users/" + id));
        try (var input = getClass().getResourceAsStream("/contracts/kotlin-json.json")) {
            assertNotNull(input);
            assertEquals(mapper.readTree(input), mapper.valueToTree(snapshots));
        }
    }

    @Test
    void optionalReportDescriptionRemainsNullable() throws Exception {
        var report =
                mapper.readValue(
                        "{\"reportedUserId\":\"00000000-0000-0000-0000-000000000102\",\"reason\":\"SPAM\"}",
                        ReportRequest.class);
        assertNull(report.description());
        assertEquals(ReportReason.SPAM, report.reason());
    }
}
