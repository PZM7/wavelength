import { Image, Linking, Pressable, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import Ionicons from '@expo/vector-icons/Ionicons';
import FontAwesome from '@expo/vector-icons/FontAwesome';
import { router } from 'expo-router';
import { useQuery } from '@tanstack/react-query';
import { useApi } from '../../src/api/useApi';
import { Screen, Title, Body, Card, Button, Eyebrow, QueryState, colors } from '../../src/components/ui';

const scoreLabels: Record<string, string> = { discovery: 'Descubre', diversity: 'Diversidad', obscurity: 'Rareza', nostalgia: 'Nostalgia' };
const scoreColors = [colors.pink, colors.accent, colors.blue, '#E8A16C'];

export default function MusicDNA() {
  const api = useApi();
  const query = useQuery({ queryKey: ['dna'], queryFn: api.dna });
  const tracks = useQuery({ queryKey: ['top-tracks'], queryFn: api.topTracks });
  const dna = query.data;
  const scores = dna ? Object.entries(dna.scores).filter(([key]) => key in scoreLabels) : [];
  return <Screen header="Tu Music DNA" right={<Text style={local.live}>{dna?.status === 'DEMO' ? 'DEMO' : 'LIVE'}</Text>}>
    <View style={local.intro}><Eyebrow>ARQUETIPO</Eyebrow><Title>{dna?.archetype === 'THE_EXPLORER' ? 'The Explorer' : dna?.archetype ?? 'Tu identidad musical'}</Title>
      <Body>Te mueves entre escenas, sigues la curiosidad y haces tuyos los sonidos nuevos.</Body></View>
    <QueryState pending={query.isPending} error={query.error} retry={() => void query.refetch()} />
    {dna?.status === 'INSUFFICIENT_DATA' && <Card><Body>Aún no hay música sincronizada. Conecta Spotify y actualiza tus gustos para construir tu perfil.</Body></Card>}
    {dna?.status === 'INSUFFICIENT_DATA' && <Button label="Sincronizar mi música" onPress={() => router.push('/(app)/music-connection')} />}
    {dna?.status === 'ARTIST_SIGNALS_ONLY' && <Card><Body>Basado en afinidades de artistas. Las dimensiones de descubrimiento y nostalgia aún no se calculan.</Body></Card>}
    {dna?.status === 'ARTIST_SIGNALS_ONLY' && <Button secondary label="Actualizar desde Spotify" onPress={() => router.push('/(app)/music-connection')} />}
    {scores.length > 0 && <View style={local.scores}>{scores.map(([key, value], index) => <Card key={key} style={local.score}>
      <Text style={local.scoreNumber}>{Math.round(value * 100)}</Text><Text style={local.scoreLabel}>{scoreLabels[key]}</Text>
      <View style={local.track}><View style={[local.fill, { width: `${Math.round(value * 100)}%`, backgroundColor: scoreColors[index % scoreColors.length] }]} /></View>
    </Card>)}</View>}
    {!!dna?.topArtists.length && <><View style={local.sectionHead}><Text style={local.sectionTitle}>Tus artistas favoritos</Text><View style={local.source}>{dna.status !== 'DEMO' && <FontAwesome name="spotify" size={13} color={colors.green} />}<Text style={local.seeAll}>{dna.status === 'DEMO' ? 'DEMO' : 'SPOTIFY'}</Text></View></View>
      <View style={local.artists}>{dna.topArtists.slice(0, 4).map(artist => <Pressable key={artist.id} style={local.artistTouch}
        accessibilityRole={artist.spotifyUrl ? 'link' : undefined} accessibilityLabel={`${artist.name} en Spotify`}
        disabled={!artist.spotifyUrl} onPress={() => { if (artist.spotifyUrl) void Linking.openURL(artist.spotifyUrl); }}><Card style={local.artistCard}>
        {artist.imageUrl ? <Image source={{ uri: artist.imageUrl }} style={local.artistOrb} resizeMode="contain" />
          : <LinearGradient colors={[colors.accent, colors.pink]} style={local.artistOrb} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} />}
        <Text numberOfLines={2} style={local.artistName}>{artist.name}</Text>
      </Card></Pressable>)}</View></>}
    <QueryState pending={tracks.isPending} error={tracks.error} retry={() => void tracks.refetch()} />
    {!!tracks.data?.length && <><View style={local.sectionHead}><Text style={local.sectionTitle}>Tus canciones más escuchadas</Text><View style={local.source}><FontAwesome name="spotify" size={13} color={colors.green} /><Text style={local.seeAll}>SPOTIFY</Text></View></View>
      <Body style={local.small}>Ordenadas por afinidad en Spotify; no es un recuento exacto de reproducciones.</Body>
      {tracks.data.slice(0, 10).map(track => <Pressable key={track.id} accessibilityRole={track.spotifyUrl ? 'link' : undefined}
        accessibilityLabel={`${track.title} de ${track.artistName} en Spotify`} disabled={!track.spotifyUrl}
        onPress={() => { if (track.spotifyUrl) void Linking.openURL(track.spotifyUrl); }}><Card style={local.songCard}>
        <Text style={local.songRank}>{track.rank}</Text>
        {track.imageUrl ? <Image source={{ uri: track.imageUrl }} style={local.songCover} resizeMode="contain" />
          : <View style={[local.songCover, { backgroundColor: colors.elevated }]} />}
        <View style={local.songText}><Text style={local.songTitle} numberOfLines={1}>{track.title}</Text>
          <Text style={local.songArtist} numberOfLines={1}>{track.artistName}</Text></View>
        <Ionicons name="open-outline" size={16} color={colors.muted} />
      </Card></Pressable>)}</>}
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
  artistTouch: { flex: 1, minWidth: 0 },
  artistCard: { padding: 7, gap: 7, borderRadius: 16 },
  artistOrb: { width: '100%', aspectRatio: 1 },
  artistName: { color: colors.ink, fontSize: 10, lineHeight: 12 },
  source: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  songCard: { flexDirection: 'row', alignItems: 'center', gap: 11, padding: 9, borderRadius: 14 },
  songRank: { color: colors.muted, width: 16, fontSize: 11, textAlign: 'center' },
  songCover: { width: 52, height: 52 },
  songText: { flex: 1, gap: 3 },
  songTitle: { color: colors.ink, fontSize: 13, fontWeight: '700' },
  songArtist: { color: colors.muted, fontSize: 11 },
  discover: { borderColor: '#644379' },
  discoverRow: { flexDirection: 'row', alignItems: 'center', gap: 11 },
  pinkDot: { width: 35, height: 35, borderRadius: 18, backgroundColor: colors.pink },
  small: { fontSize: 10 },
});
