# Migración del backend a Java 21

## Alcance e inventario previo

Se sustituyeron 27 fuentes Kotlin de producción y 4 archivos de tests. Los dos
README internos de activity y common/jobs se trasladaron al árbol Java.
Dominios conservados: auth, users, music, matching, social, concerts, activity,
common y config. Sin nuevas funciones de producto ni cambios de esquema.

Se retiraron los plugins Kotlin JVM/Spring/JPA 2.2.21, allOpen, la opción
-Xjsr305=strict y la configuración de daemon Kotlin. También kotlin-reflect,
jackson-module-kotlin y MockK 1.14.6; se elimina la dependencia de stdlib asociada
al código propio.
Mockito ya llega con spring-boot-starter-test, sin fijar una versión nueva.

Se mantienen Spring Boot **3.5.16**, Gradle **8.14.3**, dependency-management
**1.1.7**, Springdoc **2.8.16** y las demás dependencias. build.gradle.kts y
settings.gradle.kts se conservan como scripts Gradle: no son fuentes ni
dependencias Kotlin del backend. Plugin java, toolchain y release Java 21,
UTF-8 y -parameters para los parámetros de Spring MVC.

## Archivos reemplazados

| Fuente sustituido (relativo a src/) | Clases Java en el mismo package |
| --- | --- |
| main/kotlin/com/wavelength/auth/CurrentUserProvider.kt | `CurrentUserProvider.java` |
| main/kotlin/com/wavelength/common/ApiError.kt | `ApiError.java`, `ApiException.java`, `ApiExceptionHandler.java` |
| main/kotlin/com/wavelength/common/CacheStore.kt | `CacheStore.java` |
| main/kotlin/com/wavelength/common/HealthController.kt | `HealthController.java` |
| main/kotlin/com/wavelength/common/ObjectStorage.kt | `ObjectStorage.java` |
| main/kotlin/com/wavelength/common/RateLimitPolicy.kt | `RateLimitPolicy.java` |
| main/kotlin/com/wavelength/common/RequestIdFilter.kt | `RequestIdFilter.java` |
| main/kotlin/com/wavelength/concerts/Concert.kt | `Concert.java`, `AttendanceStatus.java`, `AttendanceVisibility.java`, `ConcertAttendance.java` |
| main/kotlin/com/wavelength/config/DevSeed.kt | `DevSeed.java` |
| main/kotlin/com/wavelength/config/OpenApiConfig.kt | `OpenApiConfig.java` |
| main/kotlin/com/wavelength/config/SecurityConfig.kt | `SecurityConfig.java` |
| main/kotlin/com/wavelength/matching/MatchController.kt | `MatchController.java` |
| main/kotlin/com/wavelength/matching/MatchModels.kt | `Match.java`, `MatchReason.java`, `MatchResult.java`, `MatchPage.java`, `ArtistSimilarity.java` |
| main/kotlin/com/wavelength/matching/MatchRepository.kt | `MatchRepository.java` |
| main/kotlin/com/wavelength/matching/MatchService.kt | `MatchCursor.java`, `MatchService.java` |
| main/kotlin/com/wavelength/music/MusicController.kt | `MusicAccountResponse.java`, `MusicDna.java`, `MusicDnaService.java`, `MusicController.java` |
| main/kotlin/com/wavelength/music/MusicModels.kt | `MusicProvider.java`, `MusicAccount.java`, `MusicAccountRepository.java`, `Artist.java`, `Track.java`, `ProviderArtist.java`, `ProviderTrack.java`, `UserArtistAffinity.java`, `UserTrackAffinity.java` |
| main/kotlin/com/wavelength/music/MusicProviderClient.kt | `ProviderArtistData.java`, `ProviderTrackData.java`, `ListeningData.java`, `MusicProviderClient.java`, `SpotifyMusicProviderClient.java`, `AppleMusicProviderClient.java` |
| main/kotlin/com/wavelength/music/TasteRepository.kt | `ArtistWeight.java`, `TasteRepository.java` |
| main/kotlin/com/wavelength/social/ConnectionService.kt | `ConnectionService.java` |
| main/kotlin/com/wavelength/social/PrivacyService.kt | `PrivacyService.java` |
| main/kotlin/com/wavelength/social/SocialController.kt | `ConnectionController.java`, `PublicUserController.java`, `ReportRequest.java`, `ReportResponse.java`, `ReportService.java`, `ReportController.java` |
| main/kotlin/com/wavelength/social/SocialModels.kt | `ConnectionStatus.java`, `Connection.java`, `ConnectionRepository.java`, `ConnectionResponse.java`, `ConnectionPage.java`, `Block.java`, `ReportReason.java`, `Report.java` |
| main/kotlin/com/wavelength/users/MeController.kt | `MeController.java` |
| main/kotlin/com/wavelength/users/User.kt | `User.java`, `UserRepository.java`, `UserProfile.java`, `PublicUser.java` |
| main/kotlin/com/wavelength/users/UserService.kt | `ProfileFields.java`, `UserService.java` |
| main/kotlin/com/wavelength/WavelengthApplication.kt | `WavelengthApplication.java` |
| test/kotlin/com/wavelength/ApiIntegrationTest.kt | `ApiIntegrationTest.java` |
| test/kotlin/com/wavelength/matching/MatchServiceTest.kt | `MatchServiceTest.java` |
| test/kotlin/com/wavelength/social/ConnectionServiceTest.kt | `ConnectionServiceTest.java` |
| test/kotlin/com/wavelength/users/UserServiceTest.kt | `UserServiceTest.java` |


UserMapper y ConnectionMapper reemplazan las funciones de extensión.
ApiException contiene las factorías de errores antes definidas a nivel de archivo.
TextInput conserva las reglas Unicode de espacios de las validaciones originales.
SerializationContractTest y MatchingEquivalenceTest añaden cobertura del riesgo
específico de la migración; no cambian el algoritmo ni el contrato.

## Diferencias de lenguaje necesarias

- Una clase pública por archivo; por eso aumenta el número de archivos Java sin
  cambiar los módulos. DTOs/value objects son records y sus accessors Java usan
  id()/status()/etc.; esto no altera nombres JSON.
- Entidades JPA normales, no finales, constructor protegido sin argumentos,
  propiedades privadas y getters/setters. Sin Lombok, equals/hashCode generado ni
  toString con credenciales. Las anotaciones de columnas y tablas son las mismas.
- Campos opcionales marcados @Nullable; UserRepository devuelve Optional.
  @NotNull en los campos obligatorios de ReportRequest conserva el 400 que antes
  producía Jackson Kotlin para campos ausentes/null. Descripción sigue opcional.
- @Schema(requiredProperties) conserva los campos obligatorios de OpenAPI que antes
  se inferían del tipo Kotlin. La comparación de paths y schemas forma parte del
  smoke test contra la especificación exportada antes de migrar.
- Inyección por constructor también en tests; interfaces de proveedores siguen
  vacías/stub. Las transacciones y los locks por pareja no cambian.
- Sumatoria de matching mediante bucles y LinkedHashSet/LinkedHashMap: se preservan
  orden e IEEE-754; no se usa DoubleStream.sum, que podría redondear distinto.
- APIs Java con excepciones checked declaran throws donde corresponde (URI,
  filtros Servlet y configuración Security); el handler conserva su respuesta.

## Verificación

- clean build + integrationTest desde cero en Gradle/Java 21.
- 12 unitarios migrados + 2 contratos JSON + 100 casos de matching = 114 pruebas
  sin PostgreSQL. Los resultados esperados se generaron ejecutando el JAR Kotlin
  original antes de sustituirlo; los 100 scores se comparan bit a bit.
- 11 tests Testcontainers con PostgreSQL/pgvector: firma JWT/JWKS, issuer, audiencia,
  expiración, subject, provisioning, PATCH, ranking, cursor, privacidad, reports,
  ownership y carrera entre bloquear y solicitar conexión. Ningún test omitido.
- Docker Compose build/up sobre el volumen existente; health y Actuator, rutas
  privadas rechazadas sin JWT, OpenAPI dev y Redis PING.
- Huellas SHA-256 de mobile, migraciones SQL, YAML de Spring, .env.example,
  Dockerfile, Compose y manifests npm comparadas contra la instantánea previa.
  **36 archivos comparados, 0 cambios.** Paths y components de OpenAPI:
  **0 diferencias**, incluidas las listas de campos requeridos.
- Historial/checksums Flyway y pg_dump del esquema comparados antes/después.
  Sin reset de volumen, nuevas migraciones ni cambios de IDs.
- Inspección de runtime y bootJar: sin kotlin-reflect, Jackson Kotlin ni MockK.
  Se conserva **kotlin-stdlib 1.9.25** (y sus bridges jdk7/jdk8), requerida por
  la cadena existente OpenTelemetry OTLP 1.49.0 → sender-okhttp → OkHttp 4.12.0 →
  Okio 3.6.0. Eliminarla rompería ese exportador. No se reemplaza el transporte
  de telemetría durante una migración de lenguaje. Todos los fuentes y tests
  propios del backend son Java; esta dependencia transitiva tiene una razón real.

Resultado final: **125 tests, 0 fallos, 0 omitidos**. Smoke HTTP sobre Compose:
health 200 con `{"status":"ok"}`, Actuator 200/UP, Swagger 200 y `/me`, `/matches`,
`/me/music-dna` 401 con el mismo envelope cuando falta JWT. El acceso con JWT válido
y los rechazos por firma/claims se verifican en Testcontainers, con JWKS temporal
de test. Redis devuelve `PONG`. Los tres servicios permanecen saludables.

## Comandos (desde la raíz)

PowerShell, con JAVA_HOME apuntando a Java 21:

```powershell
.\apps\backend\gradlew.bat -p apps/backend clean build
.\apps\backend\gradlew.bat -p apps/backend test integrationTest
docker compose build
docker compose up -d --wait
Invoke-RestMethod http://localhost:8080/api/v1/health
```

Alternativa Docker Desktop que compila y ejecuta toda la suite:
`.\infra\test-backend.ps1`. En Linux/macOS usar
`./apps/backend/gradlew -p apps/backend clean build integrationTest`.

Para bootRun local: `docker compose up -d postgres redis`,
`docker compose stop backend`, exportar las mismas variables de entorno
documentadas en README y ejecutar
`.\apps\backend\gradlew.bat -p apps/backend bootRun`.

## Deuda detectada y deliberadamente conservada

No se rediseñan el ranking on-demand, el SQL jOOQ sin generación, la unicidad de
conexiones tras rechazo, el soporte de un solo issuer ni las reservas de OAuth,
S3/R2, rate limiting y jobs. Se mantiene el manejo genérico previo de excepciones
HTTP no especializadas. Tampoco se actualiza Expo ni se corrigen dependencias
transitivas mobile en esta migración. Las limitaciones y la prueba pendiente en
dispositivos nativos continúan documentadas en architecture.md/verification.md.
