import { Text } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { useApi } from '../../src/api/useApi';
import { Screen, Title, Body, Card, styles, QueryState } from '../../src/components/ui';

export default function MusicConnection() {
  const api = useApi();
  const query = useQuery({ queryKey: ['accounts'], queryFn: api.accounts });
  return <Screen><Title>Donde vive{ '\n' }tu música.</Title><Body>Conectar tus servicios permitirá construir un gusto musical que te pertenece, independientemente de dónde escuches.</Body>
    <QueryState pending={query.isPending} error={query.error} retry={() => void query.refetch()} />
    {(['SPOTIFY', 'APPLE_MUSIC'] as const).map(provider => <Card key={provider}><Text style={styles.heading}>{provider === 'SPOTIFY' ? 'Spotify' : 'Apple Music'}</Text><Body>{query.data?.some(account => account.provider === provider) ? 'Cuenta registrada' : 'Sin conectar'}</Body><Body>La conexión OAuth estará disponible en una próxima fase.</Body></Card>)}
  </Screen>;
}
