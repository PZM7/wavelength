import type { Profile, MatchPage, MusicDna } from '../../types/models';
export const demoProfile: Profile = { id: 'demo-marc', username: 'marc', displayName: 'Marc', avatarUrl: null, city: 'Barcelona', birthDate: null, discoverable: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' };
export const demoMatches: MatchPage = { nextCursor: null, matches: [
  { user: { id: 'demo-lucia', username: 'lucia', displayName: 'Lucía', avatarUrl: null }, compatibility: 0.88, reasons: [{ type: 'SHARED_ARTISTS', label: '7 artistas en común', count: 7 }] },
  { user: { id: 'demo-alex', username: 'alex', displayName: 'Alex', avatarUrl: null }, compatibility: 0.54, reasons: [{ type: 'SHARED_ARTISTS', label: '6 artistas en común', count: 6 }] },
  { user: { id: 'demo-nora', username: 'nora', displayName: 'Nora', avatarUrl: null }, compatibility: 0.49, reasons: [{ type: 'SHARED_ARTISTS', label: '6 artistas en común', count: 6 }] },
] };
export const demoDna: MusicDna = { status: 'DEMO', archetype: 'THE_EXPLORER', scores: { discovery: 0.92, diversity: 0.84, obscurity: 0.73, nostalgia: 0.31 }, topArtists: ['Frank Ocean', 'Sampha', 'Jamie xx', 'Fred again..', 'Ralphie Choo', 'Judeline', 'Bicep'].map((name, i) => ({ id: `artist-${i}`, name, weight: 0.95 - i * 0.07 })) };
