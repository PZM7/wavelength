# Wavelength

Base de una app social de afinidad musical. Monorepo con Expo/React Native y un
backend Java 21/Spring Boot organizado por dominios. PostgreSQL es la fuente de
verdad; Redis y las fronteras de integración están preparados para crecer.

La migración de lenguaje, equivalencia de contratos y archivos reemplazados están
documentados en [Java 21](docs/java-migration.md). Los scripts Gradle conservan Kotlin
DSL; no hay fuentes Kotlin de backend. La stdlib transitiva de OkHttp se mantiene
porque la requiere el exportador OpenTelemetry existente.

```text
apps/
  backend/
    gradle/wrapper/            # Gradle reproducible
    src/main/java/com/wavelength/
      auth/ users/ music/ matching/ social/ concerts/ activity/
      common/ config/
    src/main/resources/db/migration/  # V1–V6
    src/test/java/com/wavelength/
    Dockerfile
  mobile/
    app/                      # Welcome, auth, tabs y perfil de match
    src/api/                  # HTTP, contratos y adaptador
    src/components/
    src/features/auth/        # sesión y SecureStore
    src/features/demo/        # fixtures explícitos
    src/types/
packages/shared/              # reserva para contratos
infra/                        # utilidades de desarrollo
docs/                         # decisiones, datos y API
docker-compose.yml
```

## Requisitos

- Docker Desktop con motor Linux y Compose v2 (para infra/tests de integración).
- Node.js 22.13+ o 24 LTS y npm. Dependencias fijadas en package-lock.json.
- Java **21** para Gradle local (el wrapper 8.14.3 no corre con Java 25).
  Alternativa: ejecutar todo el backend con Docker, sin Java local.
- Expo Go compatible con SDK 55 para explorar la UI; Google OAuth nativo requiere
  una development build. Android Emulator/iOS Simulator son opcionales; iOS nativo
  requiere macOS/Xcode o EAS. Web sirve para explorar UI.

## Arranque rápido

Desde la raíz, PowerShell:

```powershell
node infra/init-dev-env.mjs
docker compose up -d --build
Invoke-RestMethod http://localhost:8080/api/v1/health
npm ci
Copy-Item apps/mobile/.env.example apps/mobile/.env
npm run mobile
```

El generador crea `.env` una sola vez con una contraseña y una clave de cifrado aleatorias;
si ya existe, lo deja intacto. También funciona en bash. `docker compose up` puede
ejecutarse en primer plano. Flyway crea las tablas al arrancar; health devuelve `{"status":"ok"}`.
Swagger dev: <http://localhost:8080/swagger-ui/index.html>.
PostgreSQL local: `localhost:55432`; Redis: `localhost:6379`; backend: `localhost:8080`.
El puerto PostgreSQL interno sigue siendo 5432. Solo DB/Redis se publican en loopback.

En Expo pulsa `w` para web, `a` para Android, o escanea QR con una versión compatible.
En dispositivo físico usa la IP LAN del ordenador en EXPO_PUBLIC_API_URL; en emulador
Android `http://10.0.2.2:8080`. El servidor debe ser accesible por la red del dispositivo.
La app ofrece demo sin cuenta y señala los datos ficticios. El acceso con Google
está implementado, pero necesita un proyecto Supabase para activarse. Consultar
[identidad y sesión](docs/auth.md) para la configuración y las pruebas pendientes.

## Backend fuera de Docker

```powershell
docker compose up -d postgres redis
docker compose stop backend
$env:JAVA_HOME = 'C:\ruta\a\jdk-21'
$env:SPRING_PROFILES_ACTIVE = 'dev'
cd apps/backend
.\gradlew.bat bootRun
```

Linux/macOS: `./gradlew bootRun` con JAVA_HOME apuntando a Java 21.
Si Git no conserva ejecución, `chmod +x gradlew`. Spring **no carga** `.env`
automáticamente: para bootRun exportar las variables de apps/backend/.env.example
en el shell o el IDE, usando la contraseña generada en el `.env` raíz.
El `.env` raíz lo carga Compose; Expo carga apps/mobile/.env.

## Configuración

| Variables | Función |
| --- | --- |
| DATABASE_URL | JDBC PostgreSQL (`jdbc:postgresql://host:port/db`), no URL `postgres://` |
| DATABASE_USERNAME, DATABASE_PASSWORD | Credenciales DB; la contraseña debe configurarse fuera de Git |
| REDIS_HOST, REDIS_PORT | Redis (localhost:6379; Compose usa hostname redis) |
| JWT_ISSUER_URI | Issuer exacto del access token externo |
| JWT_JWK_SET_URI | Endpoint HTTPS de claves públicas del proveedor |
| JWT_AUDIENCE | Audiencia requerida; `authenticated` para Supabase |
| EXPO_PUBLIC_SUPABASE_URL, EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY | Proyecto Supabase de la app; la clave publicable no es un secreto |
| AUTH_AUTO_PROVISION | true crea usuario al primer JWT válido; false requiere alta previa |
| SPRING_PROFILES_ACTIVE | dev habilita OpenAPI y permite seed; omitir en producción |
| DEV_SEED_ENABLED | false por defecto; true + dev añade datos ficticios |
| DEV_SEED_AUTH_SUBJECT | sub real para asociar Marc, antes del primer seed; default dev\|marc |
| CORS_ALLOWED_ORIGINS | Lista separada por comas de orígenes web permitidos |
| PORT | Puerto del backend, default 8080 |
| TRACING_ENABLED | false por defecto; requiere configurar collector OTLP al activarlo |
| EXPO_PUBLIC_API_URL | URL pública del backend; nunca poner secretos en EXPO_PUBLIC_* |
| MUSIC_TOKEN_KEYS | Claves AES-256-GCM del backend; necesarias para conectar música |
| SPOTIFY_CLIENT_ID, SPOTIFY_REDIRECT_URI | OAuth PKCE de Spotify; URI exacta registrada en Spotify |
| APPLE_MUSIC_TEAM_ID/KEY_ID/PRIVATE_KEY_BASE64 | Firma del developer token MusicKit; solo backend |
| MUSIC_PUBLIC_BASE_URL, MUSIC_WEB_RETURN_URI, MUSIC_NATIVE_RETURN_URI | Página MusicKit y retorno seguro a la app |
| S3_ENDPOINT/BUCKET/ACCESS_KEY/SECRET_KEY | Reservadas; no hay uploads implementados |

Sin configurar JWT, backend/Swagger/health arrancan y las rutas privadas rechazan
acceso. Los secretos locales se guardan en archivos ignorados por Git; no se deben
poner claves privadas ni tokens en `EXPO_PUBLIC_*` ni en archivos versionados.
El CI analiza el historial Git en cada push y pull request. Consultar
[identidad y sesión](docs/auth.md) para configurar Supabase y [API](docs/api.md)
para probar `/me` y `/matches`.

Seed: editar `.env` y poner DEV_SEED_ENABLED=true, opcionalmente el subject real de
Marc **antes del primer seed**, después `docker compose up -d`. Usuarios: Marc,
Lucía, Alex, Nora; siete artistas y afinidades. No ejecutar en producción.
No hay comando de reseteo destructivo automático; `docker compose down` conserva datos.

## Verificación

```powershell
# Backend local Java 21
cd apps/backend
.\gradlew.bat clean build integrationTest
cd ../..

# Alternativa reproducible en Docker Desktop (incluye Testcontainers)
.\infra\test-backend.ps1

# Mobile
npm run typecheck
npm run test:mobile
npm run export --workspace @wavelength/mobile
```

En Linux/macOS ejecutar `./gradlew clean build integrationTest`. `test` ejecuta
unitarios JUnit 5/Mockito; `integrationTest` requiere Docker y PostgreSQL/pgvector real.
Los tests no modifican el volumen de desarrollo. No se saltan silenciosamente si
falta Docker. Reports: apps/backend/build/reports/tests/{test,integrationTest}.
El script Docker evita problemas del ejecutor de tests de Gradle en ciertas rutas
Windows con caracteres no ASCII. No ejecutar simultáneamente Gradle local y Docker
sobre el mismo directorio build.

La exportación Expo genera bundles de Android, iOS y web; no sustituye una compilación
de APK/IPA ni pruebas en dispositivos. Consultar [verificación](docs/verification.md)
para resultados comprobados y límites.

## Implementado y reservado

**Real:** Google OAuth y renovación de sesión, selección manual de artistas favoritos,
Music DNA y matching basados en esa selección, conexión musical con Spotify PKCE y
Apple Music MusicKit, cifrado de tokens y renovación Spotify (pendientes de credenciales reales),
JWT, provisioning, perfil/validación, Flyway/constraints, catálogo canónico,
afinidades, matching determinista paginado, conexiones, bloqueos, reports, JSON logs,
OpenAPI dev, health, tests PostgreSQL, UI navegable y cliente HTTP tipado.

**Stub/reserva explícita:** importación automática de gustos musicales y arquetipos DNA,
S3/R2, jobs, actividad, conciertos funcionales, embeddings, rate limiting y analytics.
La demo mobile no llama al backend ni persiste cambios. El backend DNA usa artistas
reales almacenados y devuelve dimensiones sin calcular como ausentes, no como scores falsos.

Decisiones/deuda: [arquitectura](docs/architecture.md), [base de datos](docs/database.md),
[proveedores](docs/music-providers.md), [API](docs/api.md).

## Siguientes cinco pasos

1. Crear proyecto Supabase, configurar Google y verificar login/refresh en dispositivo.
2. Configurar credenciales Spotify/Apple y probar ambos consentimientos en web y dispositivo.
3. Crear sync idempotente y reconciliación canónica para combinar datos musicales
   importados con la selección manual sin perder preferencias.
4. Validar experiencia con dispositivos reales y añadir tests E2E del flujo autenticado.
5. Preparar piloto privado: rate limiting, moderación, retención/borrado de datos,
   observabilidad conectada y medición del coste del matching.
