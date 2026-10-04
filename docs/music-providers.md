# Conexiones musicales

La autorización musical es independiente del inicio de sesión de Wavelength.
Primero se necesita una sesión Wavelength válida (Supabase Auth); después se puede
conectar Spotify o Apple Music desde **Cuentas musicales**. Los proveedores quedan
desactivados hasta configurar sus credenciales y una clave de cifrado en el backend.
No hay credenciales de proveedores en este repositorio.

## Spotify

El backend inicia Authorization Code **con PKCE S256**. Guarda `state` y el
`code_verifier` durante diez minutos en Redis; el estado se consume una sola vez.
Spotify devuelve el código al callback del backend, que intercambia el código,
consulta `/v1/me` para obtener `account_id` y guarda access y refresh tokens
cifrados. Se solicitan `user-read-private` y `user-top-read`.
Los tokens nunca se devuelven a la app.

Para activarlo, crear una app en Spotify Developer Dashboard y configurar:

```text
SPOTIFY_CLIENT_ID=<client-id>
SPOTIFY_REDIRECT_URI=http://127.0.0.1:8080/api/v1/music/spotify/callback
```

Registrar esa URI exacta en Spotify. `localhost` **no** está permitido como
redirect URI de Spotify; para producción usar una URI HTTPS pública que alcance
el backend. El flujo PKCE usa el Client ID y no requiere Client Secret. En un
dispositivo físico, `127.0.0.1` apunta al propio dispositivo: configurar un
backend HTTPS accesible y registrar allí el callback.

`POST /api/v1/me/music-connections/spotify/refresh` comprueba el access token y,
si vence dentro de un minuto, lo renueva bajo bloqueo de la fila de la cuenta.
Cuando Spotify devuelve otro refresh token, reemplaza el anterior; si no lo
devuelve, conserva el vigente. Si Spotify rechaza el refresh token, se devuelve
`MUSIC_RECONNECT_REQUIRED` y hay que volver a autorizar.

Después de conectar, **Sincronizar música de Spotify** consulta los 50
artistas principales de los periodos corto, medio y largo. Cada posición se
convierte en una señal entre 0 y 1; Music DNA y las coincidencias usan la media
de los tres periodos. Repetir la sincronización reemplaza sólo los artistas de
esa cuenta Spotify. Si Spotify rechaza la autorización,
hay que volver a autorizar; si limita peticiones, se puede reintentar más tarde.
Una respuesta incompleta no borra los gustos anteriores. También consulta las
20 canciones principales de Spotify del periodo medio y muestra sus portadas.
Estas canciones representan afinidad calculada por Spotify, no un contador de
reproducciones. Las fotos de los artistas y las portadas se muestran sin recortar,
con atribución y enlace al contenido original en Spotify. Desconectar la cuenta
borra sus artistas y canciones importados. Las escuchas recientes aún no se
importan ni se usan para calcular afinidades.

## Apple Music

Apple Music no utiliza el OAuth de Spotify. El backend crea un developer token
ES256 de una hora para una página propia con MusicKit JS. Tras el consentimiento,
MusicKit entrega un Music User Token. El backend lo valida con
`GET /v1/me/storefront` antes de cifrarlo. Apple no ofrece aquí un refresh token:
si el Music User Token deja de ser válido, el usuario vuelve a autorizar.

Crear una MusicKit key y habilitar MusicKit en Apple Developer. Configurar:

```text
APPLE_MUSIC_TEAM_ID=<team-id>
APPLE_MUSIC_KEY_ID=<key-id>
APPLE_MUSIC_PRIVATE_KEY_BASE64=<base64-del-contenido-DER-de-la-clave-p8>
MUSIC_PUBLIC_BASE_URL=http://127.0.0.1:8080
```

La clave `.p8` se convierte a base64 de sus bytes DER, sin las líneas PEM. No
incluirla en `EXPO_PUBLIC_*` ni en Git. En producción, la página MusicKit y el
backend deben usar HTTPS. Apple Music no expone un identificador de usuario estable
en este flujo; se guarda una huella del token para evitar reutilizar **ese mismo
token** en dos cuentas Wavelength. Una nueva autorización puede producir otra
huella, por lo que no se afirma unicidad de la cuenta Apple entre usuarios.

## Cifrado y retorno a la app

`MUSIC_TOKEN_KEYS` es una lista ordenada `id:base64`, con claves AES de **32 bytes**.
La primera cifra nuevas credenciales; las demás permiten leer valores anteriores.
Cada token usa AES-256-GCM con nonce aleatorio y datos autenticados que incluyen
el ID de la cuenta, proveedor y tipo de token. Las columnas `*_encrypted` nunca
guardan texto plano. `node infra/init-dev-env.mjs` genera una clave local aleatoria
en el `.env` ignorado por Git, sin imprimir su valor. Si `.env` ya existe, el
script no cambia la clave ni la contraseña.

Ejemplo de rotación: cambiar `MUSIC_TOKEN_KEYS=v1:<old>` por
`MUSIC_TOKEN_KEYS=v2:<new>,v1:<old>`. Las credenciales se recifran al listar las
cuentas musicales; Spotify también se recifra al consultar o renovar su access
token. Mantener la clave antigua hasta recifrar o reconectar todas las cuentas
inactivas; retirarla antes impide leer sus tokens. Gestionar estas claves como
secretos del entorno, con copia de seguridad y acceso restringido.

El backend elige una URI de retorno permitida, nunca una URL enviada por el
navegador. En desarrollo web:

```text
MUSIC_WEB_RETURN_URI=http://localhost:8081/music/callback
MUSIC_NATIVE_RETURN_URI=wavelength://music/callback
```

El callback devuelve únicamente resultado y proveedor; no incluye códigos ni
tokens. Desconectar borra la fila de credenciales; no invoca una revocación remota.
El usuario puede retirar también el acceso desde la configuración del proveedor.

## Alcance y verificación

El enlace y almacenamiento de credenciales están implementados. Spotify importa
artistas y canciones al solicitarlo el usuario; Apple Music aún no sincroniza gustos.
La selección manual de artistas se ha retirado de la interfaz y la API. Sus filas
históricas permanecen en la base de datos para recuperación, pero ya no participan
en Music DNA ni en las coincidencias.
La demo no conecta proveedores ni llama al backend.

Las pruebas locales cubren cifrado, rotación, estado de un solo uso, firma del
developer token Apple y contratos HTTP de ambos proveedores. En desarrollo se han
conectado cuentas reales de Spotify; el consentimiento Apple Music y los flujos
nativos aún necesitan pruebas con credenciales y dispositivos reales.
Antes de habilitar a más usuarios: probar consentimiento, retorno web y nativo,
cancelación, renovación Spotify, reconexión Apple y desconexión.

Referencias: [Spotify PKCE](https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow),
[redirect URI](https://developer.spotify.com/documentation/web-api/concepts/redirect_uri),
[renovación Spotify](https://developer.spotify.com/documentation/web-api/tutorials/refreshing-tokens),
[MusicKit](https://developer.apple.com/musickit/) y
[developer tokens Apple](https://developer.apple.com/documentation/AppleMusicAPI/generating-developer-tokens).
