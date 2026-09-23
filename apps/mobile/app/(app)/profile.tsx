import { useState, useEffect } from 'react';
import { Text, TextInput, Switch, View } from 'react-native';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useApi } from '../../src/api/useApi';
import { useSession } from '../../src/features/auth/SessionProvider';
import { Screen, Title, Body, Card, Button, QueryState, styles, colors } from '../../src/components/ui';

export default function Profile() {
  const api = useApi(); const session = useSession(); const queries = useQueryClient();
  const query = useQuery({ queryKey: ['me'], queryFn: api.me });
  const [displayName, setDisplayName] = useState(''); const [discoverable, setDiscoverable] = useState(false);
  useEffect(() => { if (query.data) { setDisplayName(query.data.displayName ?? ''); setDiscoverable(query.data.discoverable); } }, [query.data]);
  const mutation = useMutation({ mutationFn: () => api.patchProfile({ displayName: displayName || null, discoverable }), onSuccess: profile => { queries.setQueryData(['me'], profile); } });
  return <Screen><Title>Tu perfil.{ '\n' }Tus reglas.</Title><Body>Elige cómo te ven las personas que comparten tu música.</Body>
    <QueryState pending={query.isPending} error={query.error} retry={() => void query.refetch()} />
    {query.data && <Card><Text style={styles.heading}>Nombre visible</Text><TextInput accessibilityLabel="Nombre visible" value={displayName} onChangeText={setDisplayName} maxLength={80} style={styles.input} placeholderTextColor={colors.muted} />
      <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}><Body>Aparecer en afinidades</Body><Switch accessibilityLabel="Aparecer en afinidades" value={discoverable} onValueChange={setDiscoverable} trackColor={{ true: colors.accent }} thumbColor={colors.ink} /></View>
      <Button label={mutation.isPending ? 'Guardando…' : 'Guardar perfil'} disabled={session.mode === 'demo' || mutation.isPending} onPress={() => mutation.mutate()} />
      {mutation.isSuccess && <Body>Perfil guardado.</Body>}
    </Card>}
    <QueryState pending={false} error={mutation.error} />
    {session.mode === 'demo' && <Body>La demo permite explorar. Los cambios no se guardan.</Body>}
    <Button secondary label={session.mode === 'demo' ? 'Salir de la demo' : 'Cerrar sesión'} onPress={() => void session.signOut()} />
  </Screen>;
}
