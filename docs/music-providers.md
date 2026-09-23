# Proveedores musicales

`MusicProviderClient` tiene provider, isStub y getTopArtists/getTopTracks/
getRecentListening. Spotify y Apple Music son stubs explícitos que devuelven listas
vacías. No usan credenciales, no hacen llamadas externas y no se programan jobs.

La app muestra que OAuth no está implementado. `/me/music-accounts` lista solamente
cuentas guardadas, sin tokens ni providerUserId; el seed no crea cuentas ficticias.

Flujo futuro: consentimiento → OAuth de proveedor (state/PKCE según soporte) →
intercambio de token servidor → cifrado con KMS/clave gestionada → MusicAccount →
sincronización con límites → reconciliación canónica → afinidades → Music DNA.
Autenticación de Wavelength y autorización musical son procesos independientes.

No reconciliar artistas solo por nombre ni asumir que todas las canciones tienen
ISRC. ProviderArtist y ProviderTrack guardan el mapeo y pueden ampliarse sin
contaminar User o Match con IDs externos. El peso inicial es media de las ventanas;
el importador deberá definir ranking/frecuencia/decay de forma documentada.

`MusicDnaService` devuelve topArtists reales de afinidades almacenadas. Archetype
es null y scores vacío mientras no haya señales defendibles. Status informa
INSUFFICIENT_DATA o ARTIST_SIGNALS_ONLY. Solo la demo mobile tiene arquetipo y
dimensiones ficticias, identificadas como DEMO.

Antes de activar OAuth: implementar y probar cifrado/rotación, callbacks seguros,
refresh, revocación, timeouts, backoff, cuotas, consentimiento, desconexión y borrado.
Los permisos y APIs disponibles de cada proveedor deberán verificarse al integrar.
