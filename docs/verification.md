# Verificación y límites

Base verificada el 22-09-2026; migración a Java 21 verificada el 23-09-2026.
Véase [informe de migración](java-migration.md).

Actualización del 27-09-2026: la selección manual ya no tiene pantalla ni endpoint
y sus datos históricos no intervienen en DNA o matching. La sincronización Spotify
importa artistas con foto y enlaces, y 20 canciones destacadas con portada; las
pruebas de integración cubren reemplazo, consulta y desconexión. Pasaron Gradle
`test` e `integrationTest`, TypeScript, 12 pruebas Vitest y exportación Expo para
web, Android e iOS. La sincronización con una cuenta Spotify real todavía requiere
probar el botón en una sesión autenticada.

- Backend: Gradle 8.14.3 / Java 21, compilación de bootJar.
- JUnit/Mockito: 12 unitarios originales migrados, 100 casos de equivalencia bit a bit
  frente al JAR original y 2 pruebas de contratos Jackson.
- Testcontainers: 11 tests de integración (10 originales y validación de campos requeridos al retirar el módulo Jackson específico del lenguaje anterior), PostgreSQL 17 con pgvector, sin H2.
  Incluyen JWT firmado y JWKS temporal (firma/issuer/audience/exp/sub), provisioning,
  PATCH/null/constraints, SQL matching vs Java, paginación, propiedad de solicitudes,
  bloqueos bidireccionales, privacidad, reports y carrera bloque/request.
- Mobile: TypeScript strict, 8 tests Vitest de HTTP de la base, exportación Expo de web y
  bytecode Hermes para Android/iOS. Expo Doctor: 20/20 comprobaciones.
- Navegación web manual: welcome → login placeholder → demo → home → Music DNA →
  matches → perfil ajeno → perfil propio → cuentas musicales → salir. Acceso directo
  a matches sin sesión redirige al login. Sin errores de consola en ese recorrido.
  Inspección visual en viewport 390×844 y escritorio.
- Docker: construcción multi-stage y healthchecks de PostgreSQL, Redis y backend.
  Flyway crea las cinco versiones de migración. Seed solo activado en dev.

No se han generado APK/IPA ni ejecutado simuladores/dispositivos nativos. Los bundles
correctos prueban el pipeline JS/TS/Hermes, no toda la integración nativa. Tampoco
se han probado servicios OAuth externos, S3, analytics ni despliegue remoto porque
sus adaptadores/credenciales no forman parte de esta base.

El código base se publicó en el repositorio privado de GitHub. GitHub Actions quedó
configurado; comprobar su estado en el repositorio antes de depender de él.

La integración de identidad del 23-09-2026 pasó 12 tests móviles, typecheck,
exportación Expo web/iOS/Android y suite unitaria Java 21. La suite Java se ejecutó
mediante una unidad `subst` temporal con ruta ASCII por el problema de carga de
clases bajo `Programación`; Docker Desktop no estaba disponible en esta ejecución.
No se ha podido probar Google OAuth ni renovación contra Supabase porque aún no
existe el proyecto. Véase [activación de identidad](auth.md).

La conexión musical de Spotify y Apple Music del 23-09-2026 pasó las pruebas
unitarias Java de PKCE/intercambio, MusicKit/developer token, estado temporal,
cifrado y renovación de credenciales; también compiló el bootJar. Mobile pasó
typecheck, 12 tests Vitest y exportación web/iOS/Android. Se recorrió en navegador
la pantalla de cuentas de la demo: muestra ambos proveedores sin permitir enlaces
reales. No hay credenciales de Spotify, Apple Music ni proyecto Supabase, así que
el consentimiento y retorno con proveedores reales siguen sin verificar.

La selección manual de artistas del 24-09-2026 pasó la suite unitaria Java y la
suite de integración con PostgreSQL/pgvector; el caso nuevo cubre creación,
normalización, validación, aislamiento entre usuarios, Music DNA, matching y
conservación de señales existentes al borrar la selección manual. Mobile pasó
typecheck, 12 tests Vitest y exportación web/iOS/Android. La migración V6 y el
preflight CORS de PUT se comprobaron en el backend local. La pantalla se recorrió
visualmente en web en modo demo; el guardado con una sesión Google real sigue
pendiente de prueba manual en la app.

## Dependencias

`npm audit` detecta 12 avisos moderados transitivos del ecosistema Expo 55:
cadena xcode/uuid y query-string/decode-uri-component. No hay avisos altos o críticos
en esa ejecución. Vitest se actualizó a 4.1.11 para resolver su aviso.
No se aplicó `audit fix --force`: sugería bajar Expo/Router a versiones incompatibles.
Revisar la actualización compatible del SDK antes de abrir deep links no confiables
o publicar. No se presenta el proyecto como auditado para producción.

## Particularidades del equipo

- El puerto 5432 del host no estaba disponible. Compose publica 55432; no cambia
  el puerto del contenedor ni requiere reconfigurar PostgreSQL.
- Gradle local compiló, pero el ejecutor de tests falló al cargar clases bajo la
  ruta Windows con `Programación`. La suite se ejecutó en Linux mediante
  `infra/test-backend.ps1`, con volumen de caché Gradle y socket Docker para Testcontainers.
- JDK 21 descargado en `.tools/` (ignorado), sin cambiar Java global. Backend e
  infraestructura pueden ejecutarse íntegramente con Docker.

Referencias de compatibilidad:
[Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html),
[Expo SDK 55](https://docs.expo.dev/versions/v55.0.0/).
