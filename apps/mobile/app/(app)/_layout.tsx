import { Redirect, Tabs } from 'expo-router';
import Ionicons from '@expo/vector-icons/Ionicons';
import { useSession } from '../../src/features/auth/SessionProvider';
import { colors } from '../../src/components/ui';

export default function AppLayout() {
  const session = useSession();
  if (!session.ready) return null;
  if (session.mode === 'anonymous') return <Redirect href="/(auth)/login" />;
  return <Tabs screenOptions={{ headerShown: false, sceneStyle: { backgroundColor: colors.bg }, tabBarStyle: { position: 'absolute', height: 65, marginHorizontal: 12, marginBottom: 10, borderRadius: 18, backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line, overflow: 'hidden' }, tabBarActiveTintColor: colors.accent, tabBarInactiveTintColor: colors.muted, tabBarLabelStyle: { fontSize: 10, paddingBottom: 6 }, tabBarIconStyle: { marginTop: 3 } }}>
    <Tabs.Screen name="home" options={{ title: 'Inicio', tabBarIcon: ({ color }) => <Ionicons name="home-outline" size={20} color={color} /> }} />
    <Tabs.Screen name="matches" options={{ title: 'Personas', tabBarIcon: ({ color }) => <Ionicons name="people-outline" size={20} color={color} /> }} />
    <Tabs.Screen name="music-dna" options={{ title: 'Onda', tabBarIcon: ({ color }) => <Ionicons name="pulse-outline" size={20} color={color} /> }} />
    <Tabs.Screen name="chat" options={{ title: 'Chat', tabBarIcon: ({ color }) => <Ionicons name="chatbubble-ellipses-outline" size={20} color={color} /> }} />
    <Tabs.Screen name="profile" options={{ title: 'Tú', tabBarIcon: ({ color }) => <Ionicons name="person-outline" size={20} color={color} /> }} />
    <Tabs.Screen name="music-connection" options={{ href: null, tabBarStyle: { display: 'none' } }} />
    <Tabs.Screen name="match/[id]" options={{ href: null, tabBarStyle: { display: 'none' } }} />
  </Tabs>;
}
