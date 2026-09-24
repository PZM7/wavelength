# Base de datos

PostgreSQL 17 + pgvector. Flyway es dueño del DDL; Hibernate solo valida.
Fechas de evento: timestamptz/Instant UTC; cumpleaños: date/LocalDate.
UUID internos, defaults `gen_random_uuid()`. Sin dependencia de IDs musicales.

| Migración | Tablas y responsabilidad |
| --- | --- |
| V1 | users; sub y username únicos; username minúsculo 3–30, `[a-z0-9_]` |
| V2 | music_accounts, artists, tracks, provider_artists/tracks, user_artist/track_affinities, user_taste_embeddings; extensión vector |
| V3 | matches; pareja ordenada, scores 0–1, señales ausentes nullable |
| V4 | connections, blocks, reports; pareja única no dirigida, no self-links |
| V5 | concerts, concert_attendances; identidad de evento por proveedor |
| V6 | user_manual_artist_preferences; elección separada de las señales importadas |

Índices cubren FKs, filtros por usuario/proveedor y ordenaciones de matching,
solicitudes y conciertos. Nombres normalizados de artistas no son únicos: puede
haber homónimos. ISRC opcional e indexado, no único: primero definir reconciliación
y versiones de grabaciones. La representación inicial de Track tiene un artista
principal; colaboraciones/múltiples créditos quedan pendientes de su alcance.

Afinidades almacenan 3 scores no nulos con CHECK 0–1; PostgreSQL también rechaza NaN
y valores infinitos por estos checks. La PK es `(user_id, artist_id/track_id)`.
ProviderArtist/Track tienen UNIQUE(provider, provider_id). Una cuenta de cada
proveedor por usuario; una cuenta externa no puede pertenecer a varios usuarios.
Añadir proveedor requiere ampliar enum y CHECK mediante una nueva migración.
La selección manual tiene PK `(user_id, artist_id)` y se combina en consultas con
afinidades importadas tomando el mayor peso por artista. No modifica las tablas de
afinidades del proveedor.

Los tokens musicales se escriben solo mediante el servicio de credenciales, que
los cifra con AES-256-GCM antes de persistir. La clave vive fuera de la BD y del
repositorio; cada cifrado usa un nonce nuevo y las claves pueden rotarse. No
almacenar plaintext.

Las cascadas limpian datos dependientes de usuarios; referencias canónicas a
artistas evitan borrar catálogo que tenga tracks/conciertos asociados.

## Migraciones

`docker compose up --build` las ejecuta al arrancar. También `./gradlew bootRun`
con variables de conexión. No editar V1–V6 una vez desplegadas: crear V7+.
No usar `ddl-auto=update`, H2 ni SQL de creación paralelo.

## Seed de desarrollo

Requiere ambas condiciones: perfil `dev` y `DEV_SEED_ENABLED=true`. Runner
transaccional e idempotente con UUIDs fijos y `ON CONFLICT DO NOTHING`.
Marc, Lucía, Alex y Nora, siete artistas y afinidades. No tokens ni cuentas musicales
falsamente conectadas. Para vincular Marc al JWT de un proveedor real, establecer
`DEV_SEED_AUTH_SUBJECT` al subject **antes del primer seed en una BD vacía**.
Si el usuario ya existe, no se reasigna su identidad ni se sobrescriben sus datos.
Los subjects restantes son `dev|lucia`, `dev|alex`, `dev|nora`.
No existe un endpoint que emita tokens para estos subjects.

## Tests

Testcontainers crea su propia PostgreSQL/pgvector desechable. Los tests truncan
únicamente esa BD, nunca el volumen del Compose. Verifican migraciones, consultas,
constraints, bloques, concurrencia y persistencia. La tabla vectorial no contiene
embeddings ni índices hasta seleccionar un modelo y una dimensionalidad.
