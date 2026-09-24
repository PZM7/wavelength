import { StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { router } from 'expo-router';
import { useQuery } from '@tanstack/react-query';
import { useApi } from '../../src/api/useApi';
import { Screen, Title, Body, Card, Button, Eyebrow, QueryState, colors } from '../../src/components/ui';

const scoreLabels: Record<string, string> = { discovery: 'Descubre', diversity: 'Diversidad', obscurity: 'Rareza', nostalgia: 'Nostalgia' };
const scoreColors = [colors.pink, colors.accent, colors.blue, '#E8A16C'];

export default function MusicDNA() {
  const api = useApi();
  const query = useQuery({ queryKey: ['dna'], queryFn: api.dna });
  const dna = query.data;
  const scores = dna ? Object.entries(dna.scores).filter(([key]) => key in scoreLabels) : [];
  return <Screen header="Tu Music DNA" right={<Text style={local.live}>{dna?.status === 'DEMO' ? 'DEMO' : 'LIVE'}</Text>}>
    <View style={local.intro}><Eyebrow>ARQUETIPO</Eyebrow><Title>{dna?.archetype === 'THE_EXPLORER' ? 'The Explorer' : dna?.archetype ?? 'Tu identidad musical'}</Title>
      <Body>Te mueves entre escenas, sigues la curiosidad y haces tuyos los sonidos nuevos.</Body></View>
    <QueryState pending={query.isPending} error={query.error} retry={() => void query.refetch()} />
    {dna?.status === 'INSUFFICIENT_DATA' && <Card><Body>Aún no hay suficientes señales musicales. Tu perfil aparecerá aquí cuando haya datos disponibles.</Body></Card>}
    {dna?.status === 'INSUFFICIENT_DATA' && <Button label="Elegir mis artistas favoritos" onPress={() => router.push('/(app)/favorite-artists')} />}
    {dna?.status === 'ARTIST_SIGNALS_ONLY' && <Card><Body>Basado en afinidades de artistas. Las dimensiones de descubrimiento y nostalgia aún no se calculan.</Body></Card>}
    {dna?.status === 'ARTIST_SIGNALS_ONLY' && <Button secondary label="Editar mis artistas favoritos" onPress={() => router.push('/(app)/favorite-artists')} />}
    {scores.length > 0 && <View style={local.scores}>{scores.map(([key, value], index) => <Card key={key} style={local.score}>
      <Text style={local.scoreNumber}>{Math.round(value * 100)}</Text><Text style={local.scoreLabel}>{scoreLabels[key]}</Text>
      <View style={local.track}><View style={[local.fill, { width: `${Math.round(value * 100)}%`, backgroundColor: scoreColors[index % scoreColors.length] }]} /></View>
    </Card>)}</View>}
    {!!dna?.topArtists.length && <><View style={local.sectionHead}><Text style={local.sectionTitle}>Tus artistas favoritos</Text><Text style={local.seeAll}>EN TU GUSTO</Text></View>
      <View style={local.artists}>{dna.topArtists.slice(0, 4).map((artist, index) => <Card key={artist.id} style={local.artistCard}>
        <LinearGradient colors={[colors.accent, colors.pink]} style={local.artistOrb} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} />
        <Text numberOfLines={2} style={local.artistName}>{artist.name}</Text>
      </Card>)}</View></>}
    <Card style={local.discover}><View style={local.discoverRow}><View style={local.pinkDot} /><View style={{ flex: 1 }}><Text style={local.sectionTitle}>Personas en tu frecuencia</Text><Body style={local.small}>Artistas en común · afinidades musicales</Body></View></View></Card>
    <Button label="Encontrar mi gente →" onPress={() => router.push('/(app)/matches')} />
  </Screen>;
}

const local = StyleSheet.create({
  live: { color: colors.muted, borderColor: colors.line, borderWidth: 1, backgroundColor: colors.card, paddingHorizontal: 9, paddingVertical: 5, borderRadius: 16, fontSize: 9 },
  intro: { gap: 5 },
  scores: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 },
  score: { flexBasis: '22%', flexGrow: 1, minWidth: 70, borderRadius: 15, padding: 10, gap: 6 },
  scoreNumber: { color: colors.ink, fontWeight: '800', fontSize: 23 },
  scoreLabel: { color: colors.muted, fontSize: 9 },
  track: { height: 3, backgroundColor: colors.line, borderRadius: 3, overflow: 'hidden', marginTop: 4 },
  fill: { height: 3, borderRadius: 3 },
  sectionHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  sectionTitle: { color: colors.ink, fontSize: 15, fontWeight: '700' },
  seeAll: { color: colors.accent, fontSize: 9, letterSpacing: 1 },
  artists: { flexDirection: 'row', gap: 7 },
  artistCard: { flex: 1, minWidth: 0, padding: 7, gap: 7, borderRadius: 16 },
  artistOrb: { width: '100%', aspectRatio: 1, borderRadius: 100 },
  artistName: { color: colors.ink, fontSize: 10, lineHeight: 12 },
  discover: { borderColor: '#644379' },
  discoverRow: { flexDirection: 'row', alignItems: 'center', gap: 11 },
  pinkDot: { width: 35, height: 35, borderRadius: 18, backgroundColor: colors.pink },
  small: { fontSize: 10 },
});
