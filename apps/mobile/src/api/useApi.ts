import { useMemo } from 'react';
import { useSession } from '../features/auth/SessionProvider';
import { createHttpClient } from './http';
import type { MatchPageDto } from './contracts';
import type { Profile, ProfilePatch, MusicAccount, MusicDna, User, Connection, MatchPage } from '../types/models';
import { demoProfile, demoMatches, demoDna } from '../features/demo/data';

export function useApi() {
  const session = useSession();
  return useMemo(() => {
    const demo = session.mode === 'demo';
    const http = createHttpClient({
      baseUrl: process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080',
      getToken: session.getAccessToken,
      onUnauthorized: async () => { try { await session.signOut(); } catch { /* Preserve the 401 for the caller. */ } },
    });
    const realOnly = () => { if (demo) throw new Error('Esta acción necesita una sesión real. La demo no guarda cambios.'); };
    return {
      me: (): Promise<Profile> => demo ? Promise.resolve(demoProfile) : http('/api/v1/me'),
      dna: (): Promise<MusicDna> => demo ? Promise.resolve(demoDna) : http('/api/v1/me/music-dna'),
      accounts: (): Promise<MusicAccount[]> => demo ? Promise.resolve([]) : http('/api/v1/me/music-accounts'),
      musicAvailability: (): Promise<{ spotify: boolean; appleMusic: boolean }> => demo
        ? Promise.resolve({ spotify: false, appleMusic: false })
        : http('/api/v1/me/music-connections/availability'),
      startMusicConnection: (provider: MusicAccount['provider'], target: 'web' | 'native'): Promise<{ authorizationUrl: string; returnUri: string }> => {
        realOnly();
        return http(`/api/v1/me/music-connections/${provider}/start?target=${target}`, { method: 'POST' });
      },
      disconnectMusic: (provider: MusicAccount['provider']): Promise<void> => {
        realOnly();
        return http(`/api/v1/me/music-connections/${provider}`, { method: 'DELETE' });
      },
      refreshSpotify: (): Promise<void> => {
        realOnly();
        return http('/api/v1/me/music-connections/spotify/refresh', { method: 'POST' });
      },
      matches: async (cursor?: string): Promise<MatchPage> => {
        if (demo) return demoMatches;
        const dto = await http<MatchPageDto>(`/api/v1/matches?limit=20${cursor ? `&cursor=${encodeURIComponent(cursor)}` : ''}`);
        return { nextCursor: dto.nextCursor, matches: dto.matches.map(match => ({ user: { ...match.user }, compatibility: match.compatibility, reasons: match.reasons.map(reason => ({ ...reason })) })) };
      },
      user: (id: string): Promise<User> => {
        if (demo) { const user = demoMatches.matches.find(m => m.user.id === id)?.user; return user ? Promise.resolve(user) : Promise.reject(new Error('Perfil no encontrado')); }
        return http(`/api/v1/users/${encodeURIComponent(id)}`);
      },
      patchProfile: (patch: ProfilePatch): Promise<Profile> => { realOnly(); return http('/api/v1/me', { method: 'PATCH', body: JSON.stringify(patch) }); },
      connect: (id: string): Promise<Connection> => { realOnly(); return http(`/api/v1/connections/${encodeURIComponent(id)}`, { method: 'POST' }); },
      block: (id: string): Promise<void> => { realOnly(); return http(`/api/v1/users/${encodeURIComponent(id)}/block`, { method: 'POST' }); },
    };
  }, [session.mode, session.signOut]);
}
