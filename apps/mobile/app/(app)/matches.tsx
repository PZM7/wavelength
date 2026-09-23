import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { useInfiniteQuery } from '@tanstack/react-query';
import { useApi } from '../../src/api/useApi';
import { useSession } from '../../src/features/auth/SessionProvider';
import { Screen, Title, Body, Card, Button, Pill, Avatar, QueryState, colors } from '../../src/components/ui';

const demoTaste: Record<string, { artists: string; tags: string[]; color: string }> = {
  'demo-lucia': { artists: 'Frank Ocean · Sampha · FKA twigs', tags: ['alt R&B', 'art pop'], color: colors.pink },
  'demo-alex': { artists: 'Bicep · Four Tet · Jamie xx', tags: ['electrónica', 'descubrimiento'], color: colors.accent },
  'demo-nora': { artists: 'Judeline · Mk.gee · The Marías', tags: ['indie', 'dream pop'], color: colors.blue },
};

export default function Matches() {
  const api = useApi();
  const { mode } = useSession();
  const [filter, setFilter] = useState<'all' | 'near' | 'taste'>('all');
  const query = useInfiniteQuery({ queryKey: ['matches'], initialPageParam: undefined as string | undefined, queryFn: ({ pageParam }) => api.matches(pageParam), getNextPageParam: page => page.nextCursor ?? undefined });
  const matches = query.data?.pages.flatMap(page => page.matches) ?? [];
  const visibleMatches = filter === 'near' ? [] : filter === 'taste' ? matches.filter(match => match.compatibility >= 0.75) : matches;
  return <Screen right={<Text style={{ color: colors.muted, fontSize: 18 }}>⌕</Text>}>
    <Title>Personas en tu{ '\n' }frecuencia</Title><Body>Conoce a quienes comparten tu gusto musical.</Body>
    <View style={local.filters}><Pill label="Para ti" selected={filter === 'all'} onPress={() => setFilter('all')} /><Pill label="Cerca de ti" selected={filter === 'near'} onPress={() => setFilter('near')} /><Pill label="Mismo gusto" selected={filter === 'taste'} onPress={() => setFilter('taste')} /></View>
    <QueryState pending={query.isPending} error={query.error} retry={() => void query.refetch()} />
    {filter === 'near' && <Card><Body>La búsqueda por cercanía estará disponible cuando se puedan compartir ubicaciones.</Body></Card>}
    {!query.isPending && !query.error && filter !== 'near' && visibleMatches.length === 0 && <Card><Body>{filter === 'taste' ? 'No hay afinidades superiores al 75% en esta lista.' : 'Aún no hay afinidades. Necesitamos artistas en común con otras personas.'}</Body></Card>}
    {visibleMatches.map((match, index) => {
      const name = match.user.displayName ?? match.user.username ?? 'Oyente';
      const taste = mode === 'demo' ? demoTaste[match.user.id] : undefined;
      const tint = taste?.color ?? [colors.pink, colors.accent, colors.blue][index % 3]!;
      return <Pressable key={match.user.id} accessibilityRole="button" accessibilityLabel={`Ver perfil de ${name}, ${Math.round(match.compatibility * 100)}% de afinidad`} onPress={() => router.push({ pathname: '/(app)/match/[id]', params: { id: match.user.id } })}>
        <Card style={[local.matchCard, { borderColor: index === 0 ? '#654279' : colors.line }]}>
          <View style={[local.accentBar, { backgroundColor: tint }]} />
          <View style={local.matchHead}><Avatar name={name} size={48} tint={tint} /><View style={local.identity}><Text style={local.name}>{name}</Text><Text style={local.handle}>@{match.user.username ?? 'oyente'}</Text></View><Text style={local.percent}>{Math.round(match.compatibility * 100)}%</Text></View>
          <Body style={local.artists}>{taste?.artists ?? match.reasons.map(reason => reason.label).join(' · ')}</Body>
          <View style={local.tags}>{(taste?.tags ?? match.reasons.map(reason => reason.label)).map(tag => <Pill key={tag} label={tag} />)}</View>
        </Card>
      </Pressable>;
    })}
    {filter !== 'near' && query.hasNextPage && <Button disabled={query.isFetchingNextPage} label="Ver más" secondary onPress={() => void query.fetchNextPage()} />}
    <Body style={local.disclaimer}>La afinidad compara gustos compartidos. No predice cómo será vuestra relación.</Body>
  </Screen>;
}

const local = StyleSheet.create({
  filters: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 },
  matchCard: { minHeight: 140, overflow: 'hidden', paddingLeft: 19 },
  accentBar: { position: 'absolute', left: 0, top: 17, bottom: 17, width: 3, borderRadius: 2 },
  matchHead: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  identity: { flex: 1, gap: 3 },
  name: { color: colors.ink, fontSize: 17, fontWeight: '700' },
  handle: { color: colors.quiet, fontSize: 11 },
  percent: { color: colors.ink, fontSize: 25, fontWeight: '800' },
  artists: { fontSize: 11, color: colors.muted },
  tags: { flexDirection: 'row', flexWrap: 'wrap', gap: 5 },
  disclaimer: { fontSize: 11, marginTop: 8 },
});
