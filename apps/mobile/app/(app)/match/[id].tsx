import { Pressable, StyleSheet, Text, View } from 'react-native';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import { useCallback, useState } from 'react';
import Ionicons from '@expo/vector-icons/Ionicons';
import { useMutation, useQuery, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { useApi } from '../../../src/api/useApi';
import { useSession } from '../../../src/features/auth/SessionProvider';
import { demoMatches } from '../../../src/features/demo/data';
import type { MatchPage, ReportReason } from '../../../src/types/models';
import { ApiError } from '../../../src/api/http';
import { Screen, Title, Body, Card, Button, Pill, Avatar, QueryState, Eyebrow, colors } from '../../../src/components/ui';

export default function MatchProfile() {
  const params = useLocalSearchParams<{ id: string }>();
  const id = typeof params.id === 'string' ? params.id : '';
  const api = useApi(); const session = useSession(); const queries = useQueryClient();
  const [reportReason, setReportReason] = useState<ReportReason | null>(null);
  const query = useQuery({ queryKey: ['user', id], queryFn: () => api.user(id), enabled: Boolean(id) });
  const demo = session.mode === 'demo';
  const connectionQuery = useQuery({ queryKey: ['connection', id], queryFn: () => api.connectionWith(id), enabled: Boolean(id) && !demo });
  useFocusEffect(useCallback(() => { if (!demo && id) void queries.invalidateQueries({ queryKey: ['connection', id] }); }, [demo, id, queries]));
  const connect = useMutation({ mutationFn: () => api.connect(id), onSuccess: connection => { queries.setQueryData(['connection', id], connection); }, onError: error => {
    if (error instanceof ApiError && error.status === 409) void queries.invalidateQueries({ queryKey: ['connection', id] });
  } });
  const respond = useMutation({ mutationFn: ({ connectionId, response }: { connectionId: string; response: 'accept' | 'reject' }) => api.respondToConnection(connectionId, response), onSuccess: connection => { queries.setQueryData(['connection', id], connection); } });
  const block = useMutation({ mutationFn: () => api.block(id), onSuccess: async () => {
    queries.removeQueries({ queryKey: ['user', id] });
    await queries.invalidateQueries({ queryKey: ['matches'] });
    await queries.invalidateQueries({ queryKey: ['connections'] });
    await queries.invalidateQueries({ queryKey: ['blocked-users'] });
    router.replace('/(app)/matches');
  } });
  const report = useMutation({ mutationFn: (reason: ReportReason) => api.report(id, reason) });
  const connection = connectionQuery.data;
  const cached = queries.getQueryData<InfiniteData<MatchPage>>(['matches']);
  const match = (demo ? demoMatches.matches : cached?.pages.flatMap(page => page.matches) ?? []).find(item => item.user.id === id);
  const name = query.data?.displayName ?? query.data?.username ?? 'Perfil musical';
  const score = match ? Math.round(match.compatibility * 100) : null;
  const shared = match?.reasons.find(reason => reason.type === 'SHARED_ARTISTS');

  return <Screen header={false} right={null}>
    <View style={local.top}><Pressable accessibilityRole="button" accessibilityLabel="Volver a personas" onPress={() => router.replace('/(app)/matches')}><Ionicons name="arrow-back" color={colors.muted} size={22} /></Pressable><Ionicons name="ellipsis-horizontal" color={colors.muted} size={22} /></View>
    <QueryState pending={query.isPending} error={query.error} retry={() => void query.refetch()} />
    {query.data && <><Card style={local.hero}>
      <View style={local.person}><Avatar name={name} size={74} /><View style={local.identity}><Title style={local.name}>{name}</Title><Body>@{query.data.username ?? 'oyente'}</Body><Eyebrow>PERSONA EN TU FRECUENCIA</Eyebrow></View></View>
      <View style={local.heroBottom}><Body style={local.quote}>{demo && id === 'demo-lucia' ? '«Encontrando belleza en lo inesperado»' : 'Una conexión empieza con algo que escuchas.'}</Body>
        {score !== null && <View style={local.scoreRing}><Text style={local.scoreText}>{score}%</Text></View>}</View>
    </Card>
      <Text style={local.sectionTitle}>Por qué conectáis</Text>
      {shared && <Card style={local.reason}><View style={local.reasonRow}><View style={local.dot} /><Text style={local.reasonTitle}>{shared.count ?? 'Varios'} artistas en común</Text></View><Body style={local.reasonCopy}>{demo && id === 'demo-lucia' ? 'Frank Ocean, Sampha y más aparecen en las dos rotaciones.' : shared.label}</Body></Card>}
      {match?.reasons.filter(reason => reason !== shared).map(reason => <Card key={reason.type} style={local.reason}><View style={local.reasonRow}><View style={[local.dot, { backgroundColor: colors.accent }]} /><Text style={local.reasonTitle}>{reason.label}</Text></View></Card>)}
      {demo && <><Card style={local.reason}><View style={local.reasonRow}><View style={[local.dot, { backgroundColor: colors.accent }]} /><Text style={local.reasonTitle}>Un universo musical cercano</Text></View><Body style={local.reasonCopy}>Artistas y escenas que se cruzan en vuestros gustos.</Body></Card>
        <Card style={local.reason}><View style={local.reasonRow}><View style={[local.dot, { backgroundColor: colors.blue }]} /><Text style={local.reasonTitle}>Podéis sorprenderos</Text></View><Body style={local.reasonCopy}>Una afinidad abre la puerta a nuevos descubrimientos.</Body></Card></>}
      {match && <Card style={local.shared}><Body style={local.small}>Compartís una afinidad musical del {score}%.</Body></Card>}
      <QueryState pending={connectionQuery.isPending && !demo} error={connectionQuery.error} retry={() => void connectionQuery.refetch()} />
      {connection?.status === 'PENDING' && connection.requesterId === id && <>
        <Body>{name} quiere conectar contigo.</Body>
        <Button label="Aceptar solicitud" disabled={respond.isPending} onPress={() => respond.mutate({ connectionId: connection.id, response: 'accept' })} />
        <Button secondary label="Rechazar solicitud" disabled={respond.isPending} onPress={() => respond.mutate({ connectionId: connection.id, response: 'reject' })} />
      </>}
      {connection?.status === 'PENDING' && connection.requesterId !== id && <Card><Body>Solicitud enviada. Esperando respuesta.</Body></Card>}
      {connection?.status === 'ACCEPTED' && <Card><Body>Ya estáis conectados.</Body></Card>}
      {connection?.status === 'REJECTED' && <Card><Body>Esta solicitud fue rechazada.</Body></Card>}
      {(demo || (!connection && !connectionQuery.isPending && !connectionQuery.error)) && <Button label={connect.isSuccess ? 'Solicitud enviada' : `Conectar con ${name}`} disabled={demo || connect.isPending || connect.isSuccess} onPress={() => connect.mutate()} />}
      {demo && <Body style={local.small}>Las conexiones necesitan una sesión real.</Body>}
      <Button secondary label="Bloquear perfil" disabled={demo || block.isPending} onPress={() => block.mutate()} />
      {!demo && !report.isSuccess && <Card>
        <Body>¿Necesitas reportar este perfil?</Body>
        <Button secondary label={reportReason ? 'Ocultar motivos' : 'Reportar perfil'} onPress={() => setReportReason(reportReason ? null : 'SPAM')} />
        {reportReason && <>
          <View style={local.reportReasons}>{([['SPAM', 'Spam'], ['HARASSMENT', 'Acoso'], ['IMPERSONATION', 'Suplantación'], ['OTHER', 'Otro']] as const).map(([reason, label]) =>
            <Pill key={reason} label={label} selected={reportReason === reason} onPress={() => setReportReason(reason)} />)}</View>
          <Button label="Enviar reporte" disabled={report.isPending} onPress={() => report.mutate(reportReason)} />
        </>}
      </Card>}
      {report.isSuccess && <Card><Body>Reporte enviado. Gracias por avisarnos.</Body></Card>}
      <QueryState pending={false} error={(connect.error instanceof ApiError && connect.error.status === 409 ? null : connect.error) ?? respond.error ?? block.error ?? report.error} />
    </>}
  </Screen>;
}

const local = StyleSheet.create({
  top: { height: 34, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  hero: { padding: 19, minHeight: 174, borderColor: '#5F4172', gap: 16 },
  person: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  identity: { gap: 3, flex: 1 },
  name: { fontSize: 27, lineHeight: 30 },
  heroBottom: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  quote: { flex: 1, fontSize: 13 },
  scoreRing: { width: 66, height: 66, borderRadius: 33, borderWidth: 4, borderColor: colors.accent, justifyContent: 'center', alignItems: 'center' },
  scoreText: { color: colors.ink, fontWeight: '800', fontSize: 19 },
  sectionTitle: { color: colors.ink, fontWeight: '700', fontSize: 19, marginTop: 2 },
  reason: { gap: 6, paddingLeft: 18 },
  reasonRow: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  dot: { width: 7, height: 7, borderRadius: 4, backgroundColor: colors.pink },
  reasonTitle: { color: colors.ink, fontSize: 13, fontWeight: '700' },
  reasonCopy: { fontSize: 11 },
  shared: { paddingVertical: 10 },
  small: { fontSize: 11 },
  reportReasons: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 },
});
