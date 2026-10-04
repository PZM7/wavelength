import { useCallback, useEffect, useRef, useState } from 'react';
import { AppState, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router, useFocusEffect, useLocalSearchParams } from 'expo-router';
import Ionicons from '@expo/vector-icons/Ionicons';
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useApi } from '../../../src/api/useApi';
import { ApiError } from '../../../src/api/http';
import { useSession } from '../../../src/features/auth/SessionProvider';
import { Avatar, Body, Button, QueryState, colors } from '../../../src/components/ui';

const denied = (error: Error | null) => error instanceof ApiError && (error.status === 403 || error.status === 404);

export default function Conversation() {
  const params = useLocalSearchParams<{ id: string }>();
  const id = typeof params.id === 'string' ? params.id : '';
  const api = useApi();
  const { mode } = useSession();
  const queries = useQueryClient();
  const [focused, setFocused] = useState(false);
  const [active, setActive] = useState(AppState.currentState !== 'background' && AppState.currentState !== 'inactive');
  const [draft, setDraft] = useState('');
  const scroll = useRef<ScrollView>(null);
  const atBottom = useRef(true);
  const submission = useRef<{ body: string; key: string } | null>(null);
  useEffect(() => {
    const listener = AppState.addEventListener('change', state => setActive(state === 'active'));
    return () => listener.remove();
  }, []);
  useFocusEffect(useCallback(() => {
    setFocused(true);
    void queries.invalidateQueries({ queryKey: ['conversation', id] });
    void queries.invalidateQueries({ queryKey: ['messages', id] });
    return () => setFocused(false);
  }, [id, queries]));
  const enabled = Boolean(id) && mode === 'authenticated' && focused && active;
  const conversation = useQuery({ queryKey: ['conversation', id], queryFn: () => api.conversation(id), enabled });
  const me = useQuery({ queryKey: ['me'], queryFn: api.me, enabled });
  const messages = useInfiniteQuery({
    queryKey: ['messages', id], initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) => api.messages(id, pageParam),
    getNextPageParam: page => page.nextCursor ?? undefined,
    enabled: enabled && Boolean(conversation.data) && !denied(conversation.error),
    refetchInterval: query => denied(query.state.error) ? false : 3000,
    refetchIntervalInBackground: false,
  });
  const send = useMutation({
    mutationFn: (message: { body: string; key: string }) => api.sendMessage(id, message.body, message.key),
    onSuccess: async () => {
      setDraft('');
      submission.current = null;
      atBottom.current = true;
      await queries.invalidateQueries({ queryKey: ['messages', id] });
    },
  });
  const accessDenied = denied(conversation.error) || denied(messages.error) || denied(send.error);
  const name = conversation.data?.otherUser.displayName ?? conversation.data?.otherUser.username ?? 'Conversación';
  const seen = new Set<string>();
  const ordered = [...(messages.data?.pages ?? [])].reverse().flatMap(page => page.messages).filter(message => {
    if (seen.has(message.id)) return false;
    seen.add(message.id);
    return true;
  });
  function submit() {
    const body = draft.trim();
    if (!body || send.isPending || accessDenied) return;
    if (submission.current?.body !== body) submission.current = { body, key: `${Date.now()}-${Math.random().toString(36).slice(2)}` };
    send.mutate(submission.current);
  }

  return <SafeAreaView style={local.safe} edges={['top', 'bottom', 'left', 'right']}>
    <KeyboardAvoidingView style={local.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <View style={local.header}>
        <Pressable accessibilityRole="button" accessibilityLabel="Volver al chat" onPress={() => router.replace('/(app)/chat')} style={local.back}><Ionicons name="arrow-back" size={24} color={colors.ink} /></Pressable>
        <Avatar name={name} size={40} />
        <View style={local.identity}><Text style={local.name}>{name}</Text><Body style={local.subtitle}>Conectados por la música</Body></View>
      </View>
      <ScrollView ref={scroll} style={local.history} contentContainerStyle={local.messages} keyboardShouldPersistTaps="handled"
        scrollEventThrottle={100}
        onScroll={event => { const { contentOffset, contentSize, layoutMeasurement } = event.nativeEvent; atBottom.current = contentSize.height - layoutMeasurement.height - contentOffset.y < 100; }}
        onContentSizeChange={() => { if (atBottom.current) scroll.current?.scrollToEnd({ animated: false }); }}>
        {mode === 'demo' && <Body>Entra con tu cuenta y acepta una conexión para empezar a conversar.</Body>}
        <QueryState pending={conversation.isPending && mode !== 'demo'} error={conversation.error} retry={() => void conversation.refetch()} />
        <QueryState pending={me.isPending && enabled} error={me.error} retry={() => void me.refetch()} />
        <QueryState pending={messages.isPending && Boolean(conversation.data) && !accessDenied} error={messages.error} retry={() => void messages.refetch()} />
        {accessDenied && <Body>Esta conversación ya no está disponible. Revisa tus conexiones.</Body>}
        {!accessDenied && me.data && <>
          {messages.hasNextPage && <Button secondary label={messages.isFetchingNextPage ? 'Cargando…' : 'Ver mensajes anteriores'} disabled={messages.isFetchingNextPage} onPress={() => { atBottom.current = false; void messages.fetchNextPage(); }} />}
          {messages.data && ordered.length === 0 && <Body>Aún no hay mensajes. Rompe el hielo con una canción.</Body>}
          {ordered.map(message => {
            const mine = message.senderId === me.data.id;
            return <View key={message.id} style={[local.bubble, mine ? local.mine : local.theirs]}>
              <Text selectable style={local.body}>{message.body}</Text>
              <Text style={local.time}>{new Date(message.createdAt).toLocaleString('es-ES', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })}</Text>
            </View>;
          })}
        </>}
      </ScrollView>
      {!accessDenied && conversation.data && me.data && mode === 'authenticated' && <View style={local.composer}>
        <QueryState pending={false} error={send.error} />
        <View style={local.inputRow}>
          <TextInput accessibilityLabel="Mensaje" placeholder="Escribe un mensaje…" placeholderTextColor={colors.muted} value={draft} onChangeText={setDraft}
            multiline maxLength={2000} editable={!send.isPending} style={local.input} />
          <Button compact label={send.isPending ? 'Enviando…' : 'Enviar'} disabled={send.isPending || !draft.trim()} onPress={submit} />
        </View>
      </View>}
    </KeyboardAvoidingView>
  </SafeAreaView>;
}

const local = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  container: { flex: 1, width: '100%', maxWidth: 640, alignSelf: 'center' },
  header: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 16, borderBottomWidth: 1, borderColor: colors.line },
  back: { padding: 8 }, identity: { flex: 1 }, name: { color: colors.ink, fontSize: 18, fontWeight: '700' }, subtitle: { fontSize: 11 },
  history: { flex: 1 }, messages: { padding: 18, gap: 12, flexGrow: 1 },
  bubble: { maxWidth: '85%', padding: 13, borderRadius: 17, gap: 6 },
  mine: { alignSelf: 'flex-end', backgroundColor: '#59348A', borderBottomRightRadius: 4 },
  theirs: { alignSelf: 'flex-start', backgroundColor: colors.card, borderBottomLeftRadius: 4 },
  body: { color: colors.ink, fontSize: 15, lineHeight: 22 }, time: { color: colors.muted, fontSize: 10, alignSelf: 'flex-end' },
  composer: { padding: 14, gap: 10, borderTopWidth: 1, borderColor: colors.line },
  inputRow: { flexDirection: 'row', alignItems: 'flex-end', gap: 10 },
  input: { flex: 1, minHeight: 49, maxHeight: 140, padding: 13, color: colors.ink, backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line, borderRadius: 14, fontSize: 15 },
});
