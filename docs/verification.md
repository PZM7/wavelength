# Verificación y límites

Base verificada el 22-09-2026; migración a Java 21 verificada el 23-09-2026.
Véase [informe de migración](java-migration.md).

- Backend: Gradle 8.14.3 / Java 21, compilación de bootJar.
- JUnit/Mockito: 12 unitarios originales migrados, 100 casos de equivalencia bit a bit
  frente al JAR original y 2 pruebas de contratos Jackson.
- Testcontainers: 11 tests de integración (10 originales y validación de campos requeridos al retirar el módulo Jackson específico del lenguaje anterior), PostgreSQL 17 con pgvector, sin H2.
  Incluyen JWT firmado y JWKS temporal (firma/issuer/audience/exp/sub), provisioning,
  PATCH/null/constraints, SQL matching vs Java, paginación, propiedad de solicitudes,
  bloqueos bidireccionales, privacidad, reports y carrera bloque/request.
- Mobile: TypeScript strict, 8 tests Vitest de HTTP, exportación Expo de web y
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

GitHub Actions queda configurado, pero no se ha ejecutado en un repositorio remoto.
No se ha creado commit ni publicado código.

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
