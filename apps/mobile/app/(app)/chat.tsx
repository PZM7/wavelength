import { useCallback } from 'react';
import { router, useFocusEffect } from 'expo-router';
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useApi } from '../../src/api/useApi';
import { useSession } from '../../src/features/auth/SessionProvider';
import type { Connection } from '../../src/types/models';
import { Screen, Title, Body, Card, Button, QueryState, Eyebrow } from '../../src/components/ui';

export default function Connections() {
  const api = useApi();
  const { mode } = useSession();
  const queries = useQueryClient();
  const me = useQuery({ queryKey: ['me'], queryFn: api.me, enabled: mode !== 'demo' });
  const list = useInfiniteQuery({
    queryKey: ['connections'], initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) => api.connections(pageParam),
    getNextPageParam: page => page.nextCursor ?? undefined,
    enabled: mode !== 'demo',
  });
  useFocusEffect(useCallback(() => {
    if (mode !== 'demo') void queries.invalidateQueries({ queryKey: ['connections'] });
  }, [mode, queries]));
  const respond = useMutation({
    mutationFn: ({ connection, action }: { connection: Connection; action: 'accept' | 'reject' }) => api.respondToConnection(connection.id, action),
    onSuccess: (_, variables) => {
      void queries.invalidateQueries({ queryKey: ['connections'] });
      void queries.invalidateQueries({ queryKey: ['connection', variables.connection.otherUser?.id] });
    },
  });
  const all = list.data?.pages.flatMap(page => page.connections) ?? [];
  const incoming = all.filter(item => item.status === 'PENDING' && item.receiverId === me.data?.id);
  const outgoing = all.filter(item => item.status === 'PENDING' && item.requesterId === me.data?.id);
  const accepted = all.filter(item => item.status === 'ACCEPTED');
  const name = (item: Connection) => item.otherUser?.displayName ?? item.otherUser?.username ?? 'Oyente';
  const open = (item: Connection) => router.push({ pathname: '/(app)/match/[id]', params: { id: item.otherUser?.id ?? (item.requesterId === me.data?.id ? item.receiverId : item.requesterId) } });

  return <Screen header="Wavelength" right={<Button compact secondary label="Actualizar" onPress={() => void list.refetch()} />}>
    <Eyebrow>CHAT Y CONEXIONES</Eyebrow><Title>Empieza una{ '\n' }conversación.</Title>
    <Body>Acepta una invitación y habla con quienes comparten tu música.</Body>
    {mode === 'demo' && <Card><Body>Las solicitudes y conexiones aparecen aquí cuando entras con tu cuenta.</Body></Card>}
    {mode !== 'demo' && <QueryState pending={me.isPending} error={me.error} retry={() => void me.refetch()} />}
    <QueryState pending={list.isPending && mode !== 'demo'} error={list.error} retry={() => void list.refetch()} />
    {mode !== 'demo' && me.data && !list.isPending && !list.error && <>
      <Eyebrow>SOLICITUDES RECIBIDAS · {incoming.length}</Eyebrow>
      {incoming.length === 0 && <Card><Body>No tienes solicitudes pendientes.</Body></Card>}
      {incoming.map(item => <Card key={item.id}>
        <Body>{name(item)} quiere conectar contigo.</Body>
        <Button label="Aceptar" disabled={respond.isPending} onPress={() => respond.mutate({ connection: item, action: 'accept' })} />
        <Button secondary label="Rechazar" disabled={respond.isPending} onPress={() => respond.mutate({ connection: item, action: 'reject' })} />
        <Button secondary label="Ver perfil" onPress={() => open(item)} />
      </Card>)}
      <Eyebrow>ENVIADAS · {outgoing.length}</Eyebrow>
      {outgoing.length === 0 && <Body>No hay solicitudes enviadas.</Body>}
      {outgoing.map(item => <Card key={item.id}><Body>Esperando respuesta de {name(item)}.</Body><Button secondary label="Ver perfil" onPress={() => open(item)} /></Card>)}
      <Eyebrow>CONECTADAS · {accepted.length}</Eyebrow>
      {accepted.length === 0 && <Body>Aún no tienes conexiones aceptadas.</Body>}
      {accepted.map(item => <Card key={item.id}><Body>{name(item)}</Body><Button label="Abrir chat" onPress={() => router.push({ pathname: '/(app)/conversation/[id]', params: { id: item.id } })} /><Button secondary label="Ver perfil" onPress={() => open(item)} /></Card>)}
      {list.hasNextPage && <Button secondary label="Ver más conexiones" disabled={list.isFetchingNextPage} onPress={() => void list.fetchNextPage()} />}
      <QueryState pending={false} error={respond.error} />
    </>}
    <Button label="Descubrir personas" onPress={() => router.push('/(app)/matches')} />
  </Screen>;
}
