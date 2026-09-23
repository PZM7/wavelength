import { Redirect, router } from 'expo-router';
import { View, Text, Pressable, StyleSheet } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { useSession } from '../src/features/auth/SessionProvider';
import { Screen, Title, Body, Button, colors } from '../src/components/ui';

export default function Welcome() {
  const session = useSession();
  if (!session.ready) return <Screen><Body>Cargando…</Body></Screen>;
  if (session.mode !== 'anonymous') return <Redirect href="/(app)/home" />;
  return <Screen right={<Pressable onPress={() => router.push('/(auth)/login')} accessibilityRole="button"><Text style={local.skip}>Entrar</Text></Pressable>} contentStyle={local.content}>
    <View style={local.art} accessibilityLabel="Ilustración de ondas musicales moradas y rosas">
      <View style={local.outerGlow}><View style={local.innerGlow}>
        <LinearGradient colors={['#513A87', '#B24ABA', '#E94B9C']} style={local.orb}><Text style={local.note}>♫</Text></LinearGradient>
      </View></View>
    </View>
    <Title style={local.heroTitle}>Tu música ya dice quién eres.</Title>
    <Body>Conecta tu historial musical. Conviértelo en identidad y conoce a quienes escuchan como tú.</Body>
    <View style={local.stats}><Text style={local.stat}><Text style={local.statValue}>DNA</Text>{'\n'}Tu identidad</Text><Text style={local.stat}><Text style={local.statValue}>94%</Text>{'\n'}Afinidad</Text><Text style={local.stat}><Text style={local.statValue}>LIVE</Text>{'\n'}Nuevos sonidos</Text></View>
    <View style={local.actions}><Button label="Continuar con Google" onPress={() => router.push('/(auth)/login')} /><Button secondary label="Explorar demo" onPress={session.enterDemo} /></View>
    <Text style={local.privacy}>Privado por defecto · Tú eliges qué compartir</Text>
  </Screen>;
}

const local = StyleSheet.create({
  content: { maxWidth: 430, gap: 17, minHeight: '100%' },
  skip: { color: colors.muted, fontSize: 12 },
  art: { height: 240, backgroundColor: '#100E15', borderColor: colors.line, borderWidth: 1, borderRadius: 28, alignItems: 'center', justifyContent: 'center', overflow: 'hidden' },
  outerGlow: { width: 260, height: 210, borderRadius: 130, backgroundColor: '#222B5A', alignItems: 'center', justifyContent: 'center' },
  innerGlow: { width: 195, height: 175, borderRadius: 100, backgroundColor: '#473D80', alignItems: 'center', justifyContent: 'center' },
  orb: { width: 134, height: 134, borderRadius: 67, borderWidth: 1, borderColor: '#9A72D8', alignItems: 'center', justifyContent: 'center', shadowColor: colors.pink, shadowOpacity: 0.9, shadowRadius: 30 },
  note: { color: '#FFC0EA', fontSize: 46, textShadowColor: colors.pink, textShadowRadius: 14 },
  heroTitle: { fontSize: 37, lineHeight: 40 },
  stats: { flexDirection: 'row', gap: 0, alignSelf: 'flex-start', backgroundColor: colors.card, borderColor: colors.line, borderWidth: 1, borderRadius: 14, paddingVertical: 10 },
  stat: { color: colors.muted, fontSize: 10, paddingHorizontal: 13 },
  statValue: { color: colors.ink, fontSize: 14, fontWeight: '800' },
  actions: { gap: 8, marginTop: 'auto' },
  privacy: { color: colors.quiet, fontSize: 10, textAlign: 'center' },
});
