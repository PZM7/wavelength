import React from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View, type StyleProp, type TextStyle, type ViewStyle } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useSession } from '../features/auth/SessionProvider';

export const colors = {
  bg: '#0F0C13', surface: '#17131B', card: '#1D1822', elevated: '#251D2B',
  ink: '#F7F4FA', muted: '#A9A1AF', quiet: '#746D7D', line: '#332A3A',
  accent: '#A36AFF', pink: '#E647A6', blue: '#619BFA', green: '#5BC68B', error: '#FFA9B4',
};
export const gradient = [colors.accent, colors.pink] as const;

export function Screen({ children, header = 'Wavelength', right, contentStyle }: React.PropsWithChildren<{ header?: string | false; right?: React.ReactNode; contentStyle?: StyleProp<ViewStyle> }>) {
  const { mode } = useSession();
  return <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
    <ScrollView contentContainerStyle={[styles.screen, contentStyle]} showsVerticalScrollIndicator={false}>
      {header !== false && <View style={styles.header}><Text style={styles.brand}>{header}</Text>{right}</View>}
      {mode === 'demo' && <Text style={styles.demoBadge}>DEMO · DATOS FICTICIOS</Text>}
      {children}
    </ScrollView>
  </SafeAreaView>;
}
export function Title({ children, style }: React.PropsWithChildren<{ style?: StyleProp<TextStyle> }>) { return <Text style={[styles.title, style]}>{children}</Text>; }
export function Body({ children, style }: React.PropsWithChildren<{ style?: StyleProp<TextStyle> }>) { return <Text style={[styles.body, style]}>{children}</Text>; }
export function Eyebrow({ children }: React.PropsWithChildren) { return <Text style={styles.eyebrow}>{children}</Text>; }
export function Card({ children, style }: React.PropsWithChildren<{ style?: StyleProp<ViewStyle> }>) { return <View style={[styles.card, style]}>{children}</View>; }

export function Button({ label, onPress, secondary = false, disabled = false, compact = false }: { label: string; onPress: () => void; secondary?: boolean; disabled?: boolean; compact?: boolean }) {
  return <Pressable accessibilityRole="button" accessibilityState={{ disabled }} disabled={disabled} onPress={onPress} style={({ pressed }) => [styles.buttonTouch, compact && styles.compact, { opacity: disabled ? 0.45 : pressed ? 0.75 : 1 }]}>
    {secondary ? <View style={[styles.buttonBase, styles.secondaryButton]}><Text style={styles.buttonText}>{label}</Text></View> :
      <LinearGradient colors={gradient} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={styles.buttonBase}><Text style={styles.buttonText}>{label}</Text></LinearGradient>}
  </Pressable>;
}
export function Pill({ label, selected = false, onPress }: { label: string; selected?: boolean; onPress?: () => void }) {
  return <Pressable accessibilityRole={onPress ? 'button' : undefined} onPress={onPress} style={[styles.pill, selected && styles.pillSelected]}>
    <Text style={[styles.pillText, selected && styles.pillTextSelected]}>{label}</Text>
  </Pressable>;
}
export function Avatar({ name, size = 42, tint = colors.pink }: { name: string; size?: number; tint?: string }) {
  return <LinearGradient colors={[tint, colors.accent]} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={{ width: size, height: size, borderRadius: size / 2, alignItems: 'center', justifyContent: 'center' }}>
    <Text style={{ color: colors.ink, fontSize: size * 0.43, fontWeight: '700' }}>{name.trim().charAt(0).toUpperCase()}</Text>
  </LinearGradient>;
}
export function QueryState({ pending, error, retry }: { pending: boolean; error: Error | null; retry?: () => void }) {
  if (pending) return <ActivityIndicator color={colors.accent} accessibilityLabel="Cargando" />;
  if (error) return <Card><Text accessibilityRole="alert" style={{ color: colors.error }}>{error.message}</Text>{retry && <Button label="Reintentar" secondary onPress={retry} />}</Card>;
  return null;
}
export const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  screen: { width: '100%', maxWidth: 640, alignSelf: 'center', paddingHorizontal: 22, paddingTop: 8, paddingBottom: 100, gap: 18 },
  header: { height: 32, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  brand: { color: colors.ink, fontSize: 14, fontWeight: '700', letterSpacing: -0.2 },
  demoBadge: { color: colors.green, fontSize: 10, letterSpacing: 1.1, fontWeight: '700', marginTop: -7 },
  title: { color: colors.ink, fontSize: 34, lineHeight: 37, fontWeight: '800', letterSpacing: -1.1 },
  body: { color: colors.muted, fontSize: 14, lineHeight: 21 },
  eyebrow: { color: colors.accent, fontSize: 10, letterSpacing: 1.6, fontWeight: '800' },
  card: { backgroundColor: colors.card, padding: 16, borderRadius: 20, gap: 10, borderWidth: 1, borderColor: colors.line },
  buttonTouch: { borderRadius: 14, overflow: 'hidden' },
  buttonBase: { minHeight: 49, paddingHorizontal: 18, paddingVertical: 13, justifyContent: 'center', alignItems: 'center' },
  secondaryButton: { backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line, borderRadius: 14 },
  compact: { alignSelf: 'flex-start' },
  buttonText: { color: colors.ink, fontWeight: '700', fontSize: 14 },
  pill: { borderRadius: 20, backgroundColor: colors.card, borderWidth: 1, borderColor: colors.line, paddingHorizontal: 12, paddingVertical: 7 },
  pillSelected: { backgroundColor: colors.accent, borderColor: colors.accent },
  pillText: { color: colors.muted, fontSize: 11 },
  pillTextSelected: { color: colors.ink, fontWeight: '700' },
  input: { color: colors.ink, backgroundColor: colors.surface, borderColor: colors.line, borderWidth: 1, borderRadius: 12, padding: 14, fontSize: 16 },
  heading: { color: colors.ink, fontSize: 20, fontWeight: '700' },
});
