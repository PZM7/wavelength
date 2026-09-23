import { Redirect } from 'expo-router';
import { Screen, Title, Body, Card, Button, Eyebrow } from '../../src/components/ui';
import { useSession } from '../../src/features/auth/SessionProvider';

export default function Login() {
  const session = useSession();
  if (session.mode !== 'anonymous') return <Redirect href="/(app)/home" />;
  return <Screen><Eyebrow>EMPIEZA POR TU MÚSICA</Eyebrow><Title>Encuentra tu{ '\n' }frecuencia.</Title><Body>Tu identidad musical y tus cuentas viven por separado. Tú decides qué compartir.</Body>
    <Card><Eyebrow>PRÓXIMAMENTE</Eyebrow><Body>La conexión con Spotify y Apple Music todavía no está disponible. Puedes recorrer las pantallas con datos ficticios.</Body></Card>
    <Button label="Explorar demo →" onPress={session.enterDemo} />
    <Body>La demo no conecta servicios ni guarda cambios.</Body>
  </Screen>;
}
