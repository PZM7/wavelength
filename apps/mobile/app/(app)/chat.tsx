import { router } from 'expo-router';
import { Screen, Title, Body, Card, Button, Eyebrow } from '../../src/components/ui';

export default function Chat() {
  return <Screen header="Chat"><Eyebrow>PRÓXIMAMENTE</Eyebrow><Title>Una conversación empieza con una canción.</Title>
    <Card><Body>El chat todavía no forma parte de esta versión. Mientras tanto, descubre personas con las que compartes música.</Body></Card>
    <Button label="Descubrir personas →" onPress={() => router.push('/(app)/matches')} />
  </Screen>;
}
