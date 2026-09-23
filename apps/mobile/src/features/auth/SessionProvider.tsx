import React, { createContext, useContext, useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { tokenStore } from './tokenStore';

type Mode = 'anonymous' | 'demo' | 'authenticated';
type Session = { mode: Mode; ready: boolean; enterDemo: () => void; signIn: (token: string) => Promise<void>; signOut: () => Promise<void> };
const Context = createContext<Session | null>(null);
export function SessionProvider({ children }: React.PropsWithChildren) {
  const [mode, setMode] = useState<Mode>('anonymous');
  const [ready, setReady] = useState(false);
  const queries = useQueryClient();
  useEffect(() => { void tokenStore.get().then(token => { if (token) setMode('authenticated'); }).catch(() => setMode('anonymous')).finally(() => setReady(true)); }, []);
  async function reset(next: Mode) { await queries.cancelQueries(); queries.clear(); setMode(next); }
  async function signOut() { try { await tokenStore.clear(); } finally { await reset('anonymous'); } }
  return <Context.Provider value={{ mode, ready, enterDemo: () => { void reset('demo'); }, signIn: async token => { await tokenStore.set(token); await reset('authenticated'); }, signOut }}>{children}</Context.Provider>;
}
export function useSession() { const value = useContext(Context); if (!value) throw new Error('Missing SessionProvider'); return value; }
