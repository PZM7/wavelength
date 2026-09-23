import * as WebBrowser from 'expo-web-browser';
import { router } from 'expo-router';
import { Screen, Title, Body, Button } from '../../src/components/ui';

// The web popup returns its authorization code to the window that started OAuth.
WebBrowser.maybeCompleteAuthSession();

export default function AuthCallback() {
  return <Screen header={false}><Title>Completando acceso…</Title>
    <Body>Si esta ventana no se cierra, vuelve a Wavelength e inténtalo de nuevo.</Body>
    <Button secondary label="Volver al acceso" onPress={() => router.replace('/(auth)/login')} />
  </Screen>;
}
