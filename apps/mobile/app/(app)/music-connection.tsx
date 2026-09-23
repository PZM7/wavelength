import { useState } from 'react';
import { Platform, Text, View } from 'react-native';
import * as WebBrowser from 'expo-web-browser';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useApi } from '../../src/api/useApi';
import { useSession } from '../../src/features/auth/SessionProvider';
import type { MusicAccount } from '../../src/types/models';
import { Screen, Title, Body, Card, Button, styles, QueryState, colors } from '../../src/components/ui';

const providers: { id: MusicAccount['provider']; name: string; summary: string }[] = [
  { id: 'SPOTIFY', name: 'Spotify', summary: 'Autoriza la lectura de tus artistas, canciones favoritas y escuchas recientes.' },
  { id: 'APPLE_MUSIC', name: 'Apple Music', summary: 'Autoriza con MusicKit el acceso a tu biblioteca y actividad musical.' },
];

export default function MusicConnection() {
  const api = useApi();
  const session = useSession();
  const queries = useQueryClient();
  const accounts = useQuery({ queryKey: ['accounts'], queryFn: api.accounts });
  const availability = useQuery({ queryKey: ['music-availability'], queryFn: api.musicAvailability });
  const [busy, setBusy] = useState<MusicAccount['provider'] | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const demo = session.mode === 'demo';

  async function connect(provider: MusicAccount['provider']) {
    setBusy(provider);
    setNotice(null);
    try {
      const target = Platform.OS === 'web' ? 'web' : 'native';
      const { authorizationUrl, returnUri } = await api.startMusicConnection(provider, target);
      const result = await WebBrowser.openAuthSessionAsync(authorizationUrl, returnUri);
      if (result.type === 'success') {
        const outcome = new URL(result.url).searchParams.get('music');
        if (outcome === 'connected') {
          await queries.invalidateQueries({ queryKey: ['accounts'] });
          setNotice(`${provider === 'SPOTIFY' ? 'Spotify' : 'Apple Music'} conectado.`);
        } else if (outcome !== 'cancelled') setNotice('No se pudo completar la conexión. Inténtalo de nuevo.');
      }
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'No se pudo conectar el servicio.');
    } finally { setBusy(null); }
  }

  async function disconnect(provider: MusicAccount['provider']) {
    setBusy(provider);
    setNotice(null);
    try {
      await api.disconnectMusic(provider);
      await queries.invalidateQueries({ queryKey: ['accounts'] });
      setNotice('Cuenta desconectada y credenciales borradas.');
    } catch (error) { setNotice(error instanceof Error ? error.message : 'No se pudo desconectar.'); }
    finally { setBusy(null); }
  }

  return <Screen><Title>Donde vive{ '\n' }tu música.</Title>
    <Body>Conecta tus servicios para poder construir tu gusto musical. Tú decides cuándo desconectarlos.</Body>
    <QueryState pending={accounts.isPending || availability.isPending} error={accounts.error ?? availability.error} retry={() => { void accounts.refetch(); void availability.refetch(); }} />
    {providers.map(provider => {
      const connected = accounts.data?.some(account => account.provider === provider.id) ?? false;
      const configured = provider.id === 'SPOTIFY' ? availability.data?.spotify : availability.data?.appleMusic;
      return <Card key={provider.id}><Text style={styles.heading}>{provider.name}</Text>
        <Body>{connected ? 'Conectado' : 'Sin conectar'}</Body><Body>{provider.summary}</Body>
        {!demo && !configured && <Body>Disponible al configurar este proveedor en el servidor.</Body>}
        {demo && <Body>Inicia sesión para conectar una cuenta real. La demo no guarda cambios.</Body>}
        <View style={{ gap: 8 }}>
          <Button label={busy === provider.id ? 'Espera…' : connected ? 'Volver a autorizar' : `Conectar ${provider.name}`}
            disabled={demo || !configured || busy !== null} onPress={() => void connect(provider.id)} />
          {connected && <Button secondary label={`Desconectar ${provider.name}`} disabled={busy !== null}
            onPress={() => void disconnect(provider.id)} />}
        </View>
      </Card>;
    })}
    {notice && <Text accessibilityRole="alert" style={{ color: colors.ink }}>{notice}</Text>}
    <Body>La conexión autoriza acceso; la sincronización del gusto musical llegará en la siguiente fase.</Body>
  </Screen>;
}
