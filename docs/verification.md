# Verificación actual

Esta página describe el alcance comprobable del código actual. Los cambios de
etapas anteriores se conservan en el historial Git, no como resultados vigentes.

## Comprobaciones reproducibles

Desde la raíz del proyecto:

```powershell
.\infra\test-backend.ps1
npm run typecheck
npm run test:mobile
npm run export --workspace @wavelength/mobile
```

Las pruebas Java usan PostgreSQL 17/pgvector con las migraciones Flyway reales.
Cubren JWT, perfiles y privacidad, matching y Music DNA tras el arranque real con
`dev` y `DEV_SEED_ENABLED=true` en una base recién creada,
sincronización Spotify, separación de artistas homónimos con IDs distintos,
conexiones en ambos sentidos, bandeja, bloqueos y chat: acceso de participantes,
mensajes persistentes, paginación, validación y reintentos sin duplicados.
El seed solo se activa en `dev`
con `DEV_SEED_ENABLED=true`; las cuentas musicales sintéticas no equivalen a una
autorización Spotify.

TypeScript comprueba la app; Vitest cubre el cliente HTTP y la sesión. La
exportación Expo genera bundles para web, Android e iOS. Esas comprobaciones no
sustituyen una prueba de consentimiento OAuth, APK/IPA ni navegación en un
dispositivo físico.

## Límites funcionales

- Spotify importa artistas y canciones destacadas después de conectar y
  sincronizar. Apple Music permite la autorización, pero aún no importa gustos.
- Music DNA usa los artistas disponibles; las dimensiones y arquetipos no se
  calculan todavía.
- La pestaña Chat muestra solicitudes y relaciones y abre conversaciones entre
  conexiones aceptadas. Los mensajes nuevos se consultan cada tres segundos con
  la pantalla activa. No hay notificaciones push, adjuntos ni confirmaciones de
  lectura. El usuario actualiza la bandeja al abrirla o con el botón de actualización.
- El reporte se almacena, pero no existe una consola de moderación operativa.
- No hay compilaciones nativas de distribución ni pruebas E2E autenticadas
  automatizadas en dispositivos.
- La corrección de identidad de artistas evita fusiones futuras por nombre. Las
  asociaciones históricas que ya se hubieran fusionado requieren revisión de
  datos; no pueden separarse con certeza a partir del nombre.

Los secretos locales están fuera de Git. La configuración de Supabase y Spotify
se documenta en [identidad](auth.md) y [proveedores](music-providers.md).
