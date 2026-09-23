import { useState } from 'react';
import { Redirect } from 'expo-router';
import { Text } from 'react-native';
import { Screen, Title, Body, Card, Button, Eyebrow, colors } from '../../src/components/ui';
import { useSession } from '../../src/features/auth/SessionProvider';

export default function Login() {
  const session = useSession();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  if (!session.ready) return <Screen><Body>Cargando sesión…</Body></Screen>;
  if (session.mode !== 'anonymous') return <Redirect href="/(app)/home" />;
  async function signIn() {
    setBusy(true);
    setError(null);
    try { await session.signInWithGoogle(); }
    catch (failure) { setError(failure instanceof Error ? failure.message : 'No se ha podido iniciar sesión.'); }
    finally { setBusy(false); }
  }
  return <Screen><Eyebrow>CUENTA WAVELENGTH</Eyebrow><Title>Encuentra tu{ '\n' }frecuencia.</Title>
    <Body>Accede con Google para guardar tu perfil y conectar con personas que comparten tu música.</Body>
    <Card><Eyebrow>ACCESO SEGURO</Eyebrow><Body>Wavelength no guarda contraseñas. Tu sesión se renueva automáticamente mientras sigas conectado.</Body></Card>
    <Button label={busy ? 'Abriendo Google…' : 'Continuar con Google'} disabled={busy || !session.identityConfigured} onPress={() => void signIn()} />
    {!session.identityConfigured && <Body>El acceso real estará disponible al configurar el proyecto Supabase de Wavelength.</Body>}
    {error && <Text accessibilityRole="alert" style={{ color: colors.error }}>{error}</Text>}
    <Button secondary label="Explorar demo →" onPress={session.enterDemo} />
    <Body>Spotify y Apple Music se conectarán por separado más adelante. La demo no guarda cambios.</Body>
  </Screen>;
}
