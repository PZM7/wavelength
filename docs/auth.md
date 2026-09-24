# Identidad y sesión

Wavelength usa Supabase Auth para iniciar sesión con Google y renovar el access token.
Spring Security valida cada token enviado al backend; no emite sesiones ni acepta
tokens de la demo. El proyecto Supabase real se configura en archivos locales
ignorados por Git; el repositorio no contiene sus credenciales.

## Activación

1. Crear un proyecto Supabase y copiar **Project URL** y la **publishable key**.
   En `apps/mobile/.env`, definir `EXPO_PUBLIC_SUPABASE_URL` y
   `EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY`. Nunca usar una secret key o service role
   key en variables `EXPO_PUBLIC_*`.
2. En Supabase Auth, configurar una clave de firma **asimétrica** (ES256 o RS256)
   para los JWT. La clave pública debe aparecer en
   `https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json`.
   Una clave HS256 compartida no puede validarse mediante JWKS público.
3. Habilitar Google en Supabase Auth. Crear las credenciales OAuth en Google Cloud
   y registrar allí la URL de callback que muestre Supabase:
   `https://<project-ref>.supabase.co/auth/v1/callback`. El secreto de Google se
   guarda solamente en Supabase, nunca en la app.
4. En la allowlist de redirecciones de Supabase Auth, añadir
   `wavelength://auth/callback` para la app nativa y la URL local exacta usada por
   Expo web, por ejemplo `http://localhost:8081/auth/callback`.
5. En el `.env` raíz para Docker Compose, o en el entorno del proceso Java,
   configurar `JWT_ISSUER_URI=https://<project-ref>.supabase.co/auth/v1`,
   `JWT_JWK_SET_URI=https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json`
   y `JWT_AUDIENCE=authenticated`. Mobile y backend deben apuntar al mismo proyecto.
   Reiniciar ambos procesos después de cambiar las variables.

El flujo nativo de Google necesita una **development build** con el esquema
`wavelength`; Expo Go no sirve para verificar esta redirección. La vista web permite
probar el flujo local con la URL web añadida a la allowlist.

## Comportamiento

- Supabase guarda y renueva la sesión. En iOS/Android se conserva en SecureStore;
  en web permanece solo en memoria y se pierde al recargar la página.
- La app adjunta el access token a las peticiones. Ante un 401 solicita una
  renovación y reintenta una vez. Si el backend vuelve a rechazarlo, cierra la
  sesión y limpia los datos de consultas locales.
- Cerrar sesión usa el alcance local de Supabase. Un access token ya emitido puede
  seguir siendo válido hasta su vencimiento; el backend no mantiene una lista de
  revocación. Para revocación inmediata sería necesaria una comprobación adicional
  en cada petición.
- La demo sigue aislada: no usa credenciales ni hace mutaciones al backend.
  Spotify y Apple Music son integraciones musicales distintas del proveedor de
  identidad. Sus flujos están implementados y requieren credenciales propias para
  activarse; véase [conexiones musicales](music-providers.md).

El acceso con Google y `/api/v1/me` se verificaron en web. Aún quedan pruebas
manuales de renovación de un token expirado, persistencia al reiniciar la app
nativa y cierre de sesión en dispositivo.
