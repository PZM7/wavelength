import { useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { router } from 'expo-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useApi } from '../../src/api/useApi';
import { useSession } from '../../src/features/auth/SessionProvider';
import { Screen, Title, Body, Card, Button, Pill, QueryState, colors, styles } from '../../src/components/ui';

const MAX_ARTISTS = 20;
const normalize = (name: string) => name.trim().replace(/\s+/g, ' ').normalize('NFKC').toLocaleLowerCase();

export default function FavoriteArtists() {
  const api = useApi();
  const { mode } = useSession();
  const queries = useQueryClient();
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selected, setSelected] = useState<string[]>([]);
  const [inputError, setInputError] = useState('');
  const initialized = useRef(false);
  const favorites = useQuery({ queryKey: ['favorite-artists'], queryFn: api.favoriteArtists });
  const suggestions = useQuery({ queryKey: ['artists', debouncedSearch], queryFn: () => api.searchArtists(debouncedSearch), enabled: debouncedSearch.length > 0 });

  useEffect(() => { const timer = setTimeout(() => setDebouncedSearch(search.trim()), 250); return () => clearTimeout(timer); }, [search]);
  useEffect(() => {
    if (favorites.data && !initialized.current) {
      setSelected(favorites.data.map(artist => artist.name));
      initialized.current = true;
    }
  }, [favorites.data]);

  const mutation = useMutation({
    mutationFn: () => api.saveFavoriteArtists(selected),
    onSuccess: artists => {
      setSelected(artists.map(artist => artist.name));
      queries.setQueryData(['favorite-artists'], artists);
      void queries.invalidateQueries({ queryKey: ['dna'] });
      void queries.invalidateQueries({ queryKey: ['matches'] });
    },
  });

  const add = (raw: string) => {
    if (mutation.isPending) return;
    const name = raw.trim().replace(/\s+/g, ' ');
    if (!name || name.length > 100) { setInputError('Escribe un nombre de hasta 100 caracteres.'); return; }
    if (selected.some(item => normalize(item) === normalize(name))) { setInputError('Ese artista ya está en tu lista.'); return; }
    if (selected.length >= MAX_ARTISTS) { setInputError('Puedes elegir hasta 20 artistas.'); return; }
    mutation.reset();
    setSelected(current => [...current, name]);
    setInputError('');
    setSearch('');
  };

  return <Screen right={<Pressable accessibilityRole="button" accessibilityLabel="Ir a mi perfil" onPress={() => router.replace('/(app)/profile')}><Text style={local.back}>Mi perfil</Text></Pressable>}>
    <Title>Tu música.{ '\n' }Tu gente.</Title>
    <Body>Elige hasta 20 artistas que te gustan. Con ellos construiremos tu Music DNA y buscaremos personas con gustos en común.</Body>
    <QueryState pending={favorites.isPending} error={favorites.error} retry={() => void favorites.refetch()} />
    {!favorites.isPending && !favorites.error && <>
      <Card>
        <Text style={styles.heading}>Mis artistas · {selected.length}/{MAX_ARTISTS}</Text>
        {selected.length === 0 ? <Body>Añade tu primer artista para empezar.</Body> : <View style={local.chips}>{selected.map(name =>
          <Pill key={normalize(name)} label={mode === 'demo' ? name : `${name}  ×`} selected onPress={mode === 'demo' ? undefined : () => { if (!mutation.isPending) { mutation.reset(); setSelected(current => current.filter(item => normalize(item) !== normalize(name))); } }} />
        )}</View>}
      </Card>
      {mode !== 'demo' && <Card>
        <Text style={styles.heading}>Buscar o añadir</Text>
        <TextInput accessibilityLabel="Nombre del artista" value={search} onChangeText={setSearch} onSubmitEditing={() => add(search)} editable={!mutation.isPending} maxLength={100} returnKeyType="done" placeholder="Escribe un artista" placeholderTextColor={colors.quiet} style={styles.input} />
        {search.trim().length > 0 && <Button compact label={`Añadir «${search.trim()}»`} disabled={mutation.isPending} onPress={() => add(search)} />}
        {suggestions.data?.filter(artist => !selected.some(name => normalize(name) === normalize(artist.name))).map(artist =>
          <Pill key={artist.id} label={`+ ${artist.name}`} onPress={() => add(artist.name)} />
        )}
        {suggestions.isFetching && <Body>Buscando artistas…</Body>}
        <QueryState pending={false} error={suggestions.error} retry={() => void suggestions.refetch()} />
        {!!inputError && <Text accessibilityRole="alert" style={local.error}>{inputError}</Text>}
      </Card>}
      <Button label={mutation.isPending ? 'Guardando…' : 'Guardar mis artistas'} disabled={mode === 'demo' || mutation.isPending} onPress={() => mutation.mutate()} />
      {mutation.isSuccess && <Card><Body>Selección guardada. Music DNA y afinidades se han actualizado.</Body><Button secondary label="Ver mi Music DNA" onPress={() => router.push('/(app)/music-dna')} /></Card>}
      <QueryState pending={false} error={mutation.error} />
      {mode === 'demo' && <Body>La demo muestra preferencias de ejemplo; entra con Google para guardar las tuyas.</Body>}
      <Body style={local.note}>Para aparecer en las afinidades de otras personas, activa «Aparecer en afinidades» en tu perfil.</Body>
    </>}
  </Screen>;
}

const local = StyleSheet.create({
  back: { color: colors.accent, fontWeight: '700', fontSize: 14 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  error: { color: colors.error, fontSize: 13 },
  note: { fontSize: 12 },
});
