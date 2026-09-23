package com.wavelength.matching;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class MatchingEquivalenceTest {
    @TestFactory
    List<DynamicTest> matchesOriginalKotlinResultsBitForBit() throws Exception {
        try (var input = getClass().getResourceAsStream("/contracts/kotlin-matching.json")) {
            assertNotNull(input);
            var cases = new ObjectMapper().readTree(input);
            var tests = new ArrayList<DynamicTest>();
            for (int i = 0; i < cases.size(); i++) {
                var sample = cases.get(i);
                tests.add(
                        DynamicTest.dynamicTest(
                                "original sample " + i,
                                () -> {
                                    double result =
                                            ArtistSimilarity.weightedJaccard(
                                                    weights(sample.get("a")),
                                                    weights(sample.get("b")));
                                    assertEquals(
                                            Long.parseLong(sample.get("bits").asText()),
                                            Double.doubleToLongBits(result));
                                }));
            }
            return tests;
        }
    }

    private Map<UUID, Double> weights(JsonNode node) {
        Map<UUID, Double> result = new LinkedHashMap<>();
        node.properties()
                .forEach(
                        entry ->
                                result.put(
                                        UUID.fromString(entry.getKey()),
                                        entry.getValue().asDouble()));
        return result;
    }
}
