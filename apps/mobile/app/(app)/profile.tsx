import { useState, useEffect } from 'react';
import { Text, TextInput, Switch, View } from 'react-native';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useApi } from '../../src/api/useApi';
import { router } from 'expo-router';
import { useSession } from '../../src/features/auth/SessionProvider';
import { Screen, Title, Body, Card, Button, QueryState, styles, colors } from '../../src/components/ui';

export default function Profile() {
  const api = useApi(); const session = useSession(); const queries = useQueryClient();
  const query = useQuery({ queryKey: ['me'], queryFn: api.me });
  const blocked = useQuery({ queryKey: ['blocked-users'], queryFn: api.blockedUsers, enabled: session.mode !== 'demo' });
  const [displayName, setDisplayName] = useState(''); const [discoverable, setDiscoverable] = useState(false);
  const [signOutError, setSignOutError] = useState<Error | null>(null);
  useEffect(() => { if (query.data) { setDisplayName(query.data.displayName ?? ''); setDiscoverable(query.data.discoverable); } }, [query.data]);
  const mutation = useMutation({ mutationFn: () => api.patchProfile({ displayName: displayName || null, discoverable }), onSuccess: profile => { queries.setQueryData(['me'], profile); void queries.invalidateQueries({ queryKey: ['matches'] }); } });
  const unblock = useMutation({ mutationFn: (id: string) => api.unblock(id), onSuccess: () => { void queries.invalidateQueries({ queryKey: ['blocked-users'] }); void queries.invalidateQueries({ queryKey: ['matches'] }); } });
  return <Screen><Title>Tu perfil.{ '\n' }Tus reglas.</Title><Body>Elige cómo te ven las personas que comparten tu música.</Body>
    <QueryState pending={query.isPending} error={query.error} retry={() => void query.refetch()} />
    {query.data && <Card><Text style={styles.heading}>Nombre visible</Text><TextInput accessibilityLabel="Nombre visible" value={displayName} onChangeText={setDisplayName} maxLength={80} style={styles.input} placeholderTextColor={colors.muted} />
      <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}><Body>Aparecer en afinidades</Body><Switch accessibilityLabel="Aparecer en afinidades" value={discoverable} onValueChange={setDiscoverable} trackColor={{ true: colors.accent }} thumbColor={colors.ink} /></View>
      <Button label={mutation.isPending ? 'Guardando…' : 'Guardar perfil'} disabled={session.mode === 'demo' || mutation.isPending} onPress={() => mutation.mutate()} />
      {mutation.isSuccess && <Body>Perfil guardado.</Body>}
    </Card>}
    <QueryState pending={false} error={mutation.error} />
    <Button secondary label="Conexiones musicales" onPress={() => router.push('/(app)/music-connection')} />
    {session.mode !== 'demo' && <Card><Text style={styles.heading}>Perfiles bloqueados</Text>
      <QueryState pending={blocked.isPending} error={blocked.error} retry={() => void blocked.refetch()} />
      {blocked.data?.length === 0 && <Body>No has bloqueado a nadie.</Body>}
      {blocked.data?.map(user => <View key={user.id}><Body>{user.displayName ?? user.username ?? 'Oyente'}</Body><Button secondary label="Desbloquear" disabled={unblock.isPending} onPress={() => unblock.mutate(user.id)} /></View>)}
      <QueryState pending={false} error={unblock.error} />
    </Card>}
    {session.mode === 'demo' && <Body>La demo permite explorar. Los cambios no se guardan.</Body>}
    <Button secondary label={session.mode === 'demo' ? 'Salir de la demo' : 'Cerrar sesión'} onPress={() => void session.signOut().catch(error => setSignOutError(error instanceof Error ? error : new Error('No se ha podido cerrar la sesión.')))} />
    <QueryState pending={false} error={signOutError} />
  </Screen>;
}
