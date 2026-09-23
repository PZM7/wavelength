import { useEffect } from 'react';
import { router, useLocalSearchParams } from 'expo-router';
import * as WebBrowser from 'expo-web-browser';
import { useQueryClient } from '@tanstack/react-query';
import { Screen, Title, Body, Button } from '../../src/components/ui';

WebBrowser.maybeCompleteAuthSession();

export default function MusicCallback() {
  const { music } = useLocalSearchParams<{ music?: string }>();
  const queries = useQueryClient();
  useEffect(() => { if (music === 'connected') void queries.invalidateQueries({ queryKey: ['accounts'] }); }, [music, queries]);
  return <Screen><Title>{music === 'connected' ? 'Música conectada.' : 'Conexión musical'}</Title>
    <Body>{music === 'connected' ? 'Ya puedes volver a tus cuentas musicales.' : 'Vuelve a Wavelength para intentarlo de nuevo.'}</Body>
    <Button label="Volver a mis cuentas" onPress={() => router.replace('/(app)/music-connection')} />
  </Screen>;
}
