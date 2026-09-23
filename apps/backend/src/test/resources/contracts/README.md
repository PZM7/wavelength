# Language migration fixtures

`kotlin-json.json` and `kotlin-matching.json` were generated from the original
Wavelength 0.1.0 Kotlin bootJar before it was replaced. They are expected results,
not regenerated from the Java implementation.

- JSON: private/public profiles, nullable fields, UTC timestamps, LocalDate, enums,
  Music DNA, match reasons/pages, connections, reports and errors.
- Matching: 100 deterministic pairs, Java Random seed 210921, 40 possible artist
  UUIDs per pair, insertion-ordered maps. Each case stores the exact IEEE-754 bits
  returned by the original weighted Jaccard implementation.

`SerializationContractTest` uses Spring Boot's ObjectMapper to compare JSON trees.
`MatchingEquivalenceTest` requires bit-for-bit equality, not a tolerance.
These are synthetic fixtures without credentials or personal data.
