import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import Ionicons from '@expo/vector-icons/Ionicons';
import { LinearGradient } from 'expo-linear-gradient';
import { useSession } from '../../src/features/auth/SessionProvider';
import { Screen, Title, Body, Card, Button, Pill, colors } from '../../src/components/ui';

const listening = [
  { person: 'Lucía', action: 'tiene esto en bucle', artist: 'Sampha', track: 'Only', time: '12 min', tint: colors.pink, note: 'Sigue sonando diferente cada vez.' },
  { person: 'Alex', action: 'acaba de descubrir', artist: 'Bicep', track: 'Glue', time: '28 min', tint: colors.accent, note: 'Un clásico que siempre vuelve.' },
  { person: 'Nora', action: 'ha encontrado una joya', artist: 'Judeline', track: 'Bodhiria', time: '1 h', tint: colors.blue, note: 'Esto merece estar en tu cola.' },
];

export default function Home() {
  const { mode } = useSession();
  const [liked, setLiked] = useState<string[]>([]);
  const [filter, setFilter] = useState<'all' | 'matches' | 'near'>('all');
  return <Screen header="Escuchando ahora" right={<Pressable accessibilityRole="button" accessibilityLabel="Cuentas musicales" onPress={() => router.push('/(app)/music-connection')}><Ionicons name="add" size={22} color={colors.ink} /></Pressable>}>
    <View style={local.live}><Text style={local.liveText}>● {mode === 'demo' ? 'Actividad de demostración' : 'Tu actividad musical'}</Text></View>
    <View style={local.filters}><Pill label="Todo" selected={filter === 'all'} onPress={() => setFilter('all')} /><Pill label="Afinidades" selected={filter === 'matches'} onPress={() => setFilter('matches')} /><Pill label="Cerca de ti" selected={filter === 'near'} onPress={() => setFilter('near')} /></View>
    {filter === 'near' ? <Card><Body>La actividad cercana estará disponible cuando se puedan compartir ubicaciones.</Body></Card> : mode === 'demo' ? listening.map(item => <Card key={item.person} style={local.listenCard}>
      <View style={local.listenRow}><LinearGradient colors={[item.tint, '#292134']} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={local.album} /><View style={local.listenMeta}>
        <Text style={local.micro}>{item.person} {item.action}</Text><Text style={local.artist}>{item.artist}</Text><Text style={local.track}>{item.track}</Text>
      </View><Text style={local.time}>{item.time}</Text></View>
      <View style={local.cardFooter}><Body style={local.note}>{item.note}</Body><Pressable accessibilityRole="button" accessibilityLabel={liked.includes(item.person) ? 'Quitar favorito' : 'Marcar favorito'} onPress={() => setLiked(current => current.includes(item.person) ? current.filter(name => name !== item.person) : [...current, item.person])}><Ionicons name={liked.includes(item.person) ? 'heart' : 'heart-outline'} size={18} color={liked.includes(item.person) ? colors.pink : colors.muted} /></Pressable></View>
    </Card>) : <Card><Body>La actividad de escucha aparecerá aquí cuando conectes un proveedor musical y esta función esté disponible.</Body><Button secondary label="Ver cuentas musicales" onPress={() => router.push('/(app)/music-connection')} /></Card>}
    <Card style={local.trending}><Text style={local.sectionTitle}>Tendencias en tu gusto</Text>
      <View style={local.trendRow}>{(mode === 'demo' ? [{ label: 'Mk.gee', color: colors.pink }, { label: 'FKA twigs', color: colors.accent }, { label: 'Overmono', color: colors.blue }] : []).map(artist => <View key={artist.label} style={local.trend}><View style={[local.trendDot, { backgroundColor: artist.color }]} /><Text style={local.trendLabel}>{artist.label}</Text></View>)}</View>
      {mode !== 'demo' && <Body>Tu gusto musical aparecerá aquí cuando haya datos disponibles.</Body>}
    </Card>
    <Button secondary label="Explorar mi Music DNA →" onPress={() => router.push('/(app)/music-dna')} />
  </Screen>;
}

const local = StyleSheet.create({
  live: { alignSelf: 'flex-start', backgroundColor: '#153222', paddingHorizontal: 10, paddingVertical: 5, borderRadius: 16 },
  liveText: { color: colors.green, fontWeight: '700', fontSize: 10 },
  filters: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginTop: -3 },
  listenCard: { padding: 0, gap: 0, overflow: 'hidden' },
  listenRow: { flexDirection: 'row', minHeight: 99 },
  album: { width: 82, margin: 8, borderRadius: 11, shadowColor: colors.pink, shadowOpacity: 0.4, shadowRadius: 14 },
  listenMeta: { flex: 1, justifyContent: 'center', gap: 3, paddingLeft: 5 },
  micro: { color: colors.muted, fontSize: 10 },
  artist: { color: colors.ink, fontSize: 17, fontWeight: '700' },
  track: { color: colors.muted, fontSize: 13 },
  time: { color: colors.quiet, fontSize: 10, paddingRight: 10, paddingTop: 12 },
  cardFooter: { borderTopColor: colors.line, borderTopWidth: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 11, paddingVertical: 7 },
  note: { color: colors.ink, fontSize: 11, flex: 1 },
  trending: { gap: 12 },
  sectionTitle: { color: colors.ink, fontSize: 15, fontWeight: '700' },
  trendRow: { flexDirection: 'row', gap: 8 },
  trend: { flex: 1, backgroundColor: colors.elevated, borderRadius: 12, borderColor: colors.line, borderWidth: 1, padding: 10, gap: 7 },
  trendDot: { width: 24, height: 24, borderRadius: 12 },
  trendLabel: { color: colors.ink, fontSize: 10 },
});
