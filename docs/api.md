# API v1

Base local: `http://localhost:8080/api/v1`. JSON, UTC ISO-8601, UUIDs internos.
Swagger en perfil dev: `http://localhost:8080/swagger-ui/index.html`.
OpenAPI JSON: `/v3/api-docs`. Desactivados sin perfil dev.

Todas las rutas requieren `Authorization: Bearer <JWT>` salvo health y OpenAPI
en dev. Actuator público: solo `/actuator/health` sin detalles. Validación JWT:
RS256 o ES256 por JWKS, issuer exacto, audience configurada, exp obligatorio, nbf si está
presente, sub no vacío (máximo 255). Tolerancia temporal estándar Spring de 60 s.
JWKS se obtiene de forma perezosa: el backend arranca sin proveedor, pero no acepta
ningún token hasta configurar uno válido. No usar ID tokens como access tokens.

| Método | Ruta | Respuesta |
| --- | --- | --- |
| GET | /health | 200 `{ "status": "ok" }` |
| GET | /me | 200 perfil privado; provisioning configurado |
| PATCH | /me | 200 perfil actualizado |
| GET | /artists?query=… | 200 hasta 20 artistas del catálogo |
| GET | /me/favorite-artists | 200 selección manual propia |
| PUT | /me/favorite-artists | 200 selección sustituida; body `{ "names": ["Björk"] }` |
| GET | /me/music-accounts | 200 lista segura (sin credenciales) |
| GET | /me/music-connections/availability | 200 proveedores configurados |
| POST | /me/music-connections/{provider}/start?target=web\|native | 200 authorizationUrl, returnUri |
| POST | /me/music-connections/spotify/refresh | 204; renueva cuando está por caducar |
| POST | /me/music-connections/spotify/sync | 200; importa artistas favoritos, devuelve `artistsImported` y `syncedAt` |
| DELETE | /me/music-connections/{provider} | 204; borra las credenciales |
| GET | /me/music-dna | 200 status, archetype nullable, scores, topArtists |
| GET | /matches?limit=20&cursor=… | 200 `{matches, nextCursor}` |
| GET | /users/{id} | 200 perfil público mínimo |
| POST | /connections/{userId} | 201 conexión PENDING |
| GET | /connections?limit=20&cursor=… | 200 `{connections, nextCursor}` |
| POST | /connections/{id}/accept | 200 conexión ACCEPTED |
| POST | /connections/{id}/reject | 200 conexión REJECTED |
| POST | /users/{id}/block | 204; idempotente |
| DELETE | /users/{id}/block | 204; elimina solo bloqueo propio |
| POST | /reports | 201 `{id}` |

`limit`: entero 1–50. Matches usa cursor opaco base64url (score + UUID);
connections usa UUID como cursor, ordenado por ID (no cronológico). Pasar el
`nextCursor` de la última respuesta, detenerse cuando sea null. Ambos endpoints
excluyen conexiones/personas bloqueadas. Matches sin señales devuelve lista vacía.

La selección manual admite hasta 20 nombres de 1–100 caracteres. Los nombres se
normalizan para reutilizar un artista existente y evitar duplicados por mayúsculas
o espacios. Una lista vacía borra solo la selección manual. Los gustos importados
permanecen intactos. Cada artista elegido aporta una señal de peso 1; si existe
una señal importada para el mismo artista se usa el mayor peso. Solo usuarios con
`discoverable=true` aparecen como candidatos en afinidades.
La entrada manual identifica artistas por nombre normalizado; homónimos todavía
requieren desambiguación con identificadores de proveedor o catálogo externo.

Los endpoints de inicio y desconexión musical requieren JWT Wavelength. Los
callbacks públicos `/api/v1/music/spotify/callback` y
`/api/v1/music/apple/complete` usan un estado temporal de un solo uso, vinculado
al usuario que inició el flujo. Nunca devuelven tokens al cliente. Véase
[proveedores](music-providers.md) para configuración y límites.

## Perfil y PATCH

Perfil privado: id, username, displayName, avatarUrl, city, birthDate,
discoverable, createdAt, updatedAt. Perfil público: solo id, username,
displayName y avatarUrl. Nunca externalAuthId, cumpleaños o ciudad públicos.
Perfil ajeno oculto o bloqueado: 404. Usuarios no discoverable pueden seguir
siendo visibles para sus conexiones ACCEPTED, salvo bloqueo.

```json
{
  "username": "marc_music",
  "displayName": "Marc",
  "city": "Barcelona",
  "birthDate": "1998-06-15",
  "avatarUrl": null,
  "discoverable": true
}
```

Todos los campos son opcionales en PATCH. Ausencia conserva el valor. Null elimina
username/displayName/city/birthDate/avatarUrl; discoverable debe ser booleano.
No admite campos desconocidos, id, externalAuthId ni timestamps.
Username: 3–30 caracteres minúsculos ASCII, dígitos o `_`, único. DisplayName máx.
80, city 120, avatarUrl 2048 y HTTPS sin userinfo; birthDate debe estar en el pasado.
No se descargan URLs en backend. Username ya usado: 409.

## Conexiones, bloqueos y reportes

No se puede conectar consigo mismo, con perfil oculto ni con persona bloqueada.
La pareja no dirigida es única, incluso tras rechazo. Solo receptor puede aceptar
o rechazar PENDING. Otra transición devuelve 409. Un tercero o solicitante no
puede responder (403). Bloquear rechaza la conexión e impide nuevas solicitudes
en ambos sentidos; desbloquear no recrea ni reactiva conexiones.

```json
{
  "reportedUserId": "00000000-0000-0000-0000-000000000102",
  "reason": "HARASSMENT",
  "description": "Descripción opcional, máximo 2000 caracteres"
}
```

Reason: SPAM, HARASSMENT, IMPERSONATION, OTHER. Self-report: 400. Usuario inexistente:
404. Se permite reportar después de bloquear. No hay endpoints de lectura de reports
ni panel de moderación. Aplicar rate limiting antes de exponer públicamente.

## Errores

```json
{
  "code": "USER_NOT_FOUND",
  "message": "User not found",
  "timestamp": "2026-01-01T00:00:00Z",
  "path": "/api/v1/users/00000000-0000-0000-0000-000000000102"
}
```

400 VALIDATION_ERROR; 401 UNAUTHORIZED; 403 FORBIDDEN/USER_NOT_PROVISIONED;
404 USER_NOT_FOUND/CONNECTION_NOT_FOUND; 409 RESOURCE_CONFLICT/CONNECTION_EXISTS/
INVALID_CONNECTION_STATE; 500 INTERNAL_ERROR. El cliente contempla también 429
para el futuro filtro/gateway; actualmente no existe rate limiter backend.
Cabecera X-Request-ID generada/validada y devuelta para correlación, sin stack traces.

Conexiones musicales: 400 MUSIC_CONNECT_EXPIRED/MUSIC_AUTH_INVALID;
409 MUSIC_RECONNECT_REQUIRED; 502 MUSIC_PROVIDER_ERROR;
503 MUSIC_NOT_CONFIGURED. La respuesta nunca contiene credenciales del proveedor.

## Probar con un issuer real

Configurar las 3 variables JWT para el proyecto Supabase (audiencia
`authenticated`), obtener un access token desde la app y guardarlo en una variable
de shell, no en Git. Véase [identidad](auth.md).

```powershell
$headers = @{ Authorization = "Bearer $env:WAVELENGTH_ACCESS_TOKEN" }
Invoke-RestMethod http://localhost:8080/api/v1/me -Headers $headers
Invoke-RestMethod http://localhost:8080/api/v1/matches -Headers $headers
```

Para matches con seed, configurar DEV_SEED_AUTH_SUBJECT antes del primer seed,
usando el sub exacto de ese token. Un usuario recién provisionado no tiene artistas
y devuelve lista vacía. Los tests de integración generan una clave RSA temporal
y JWKS local de test, validan tokens reales firmados y prueban seed sin credenciales
externas. Ese emisor existe solo en tests y no se empaqueta en producción.
