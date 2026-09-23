import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { identity, identityConfigured } from './identity';

type Mode = 'anonymous' | 'demo' | 'authenticated';
type Session = {
  mode: Mode;
  ready: boolean;
  identityConfigured: boolean;
  enterDemo: () => void;
  signInWithGoogle: () => Promise<boolean>;
  getAccessToken: (forceRefresh?: boolean) => Promise<string | null>;
  signOut: () => Promise<void>;
};
const Context = createContext<Session | null>(null);

export function SessionProvider({ children }: React.PropsWithChildren) {
  const [mode, setMode] = useState<Mode>('anonymous');
  const [ready, setReady] = useState(false);
  const queries = useQueryClient();
  const reset = useCallback(async (next: Mode) => {
    await queries.cancelQueries();
    queries.clear();
    setMode(next);
  }, [queries]);

  useEffect(() => {
    let mounted = true;
    const unsubscribe = identity.subscribe((authenticated, clearQueries) => {
      if (!mounted) return;
      if (clearQueries) void reset(authenticated ? 'authenticated' : 'anonymous');
      else setMode(authenticated ? 'authenticated' : 'anonymous');
    });
    const stopWatching = identity.watchAppState();
    void identity.getAccessToken().then(token => {
      if (mounted) setMode(token ? 'authenticated' : 'anonymous');
    }).catch(() => {
      if (mounted) setMode('anonymous');
    }).finally(() => {
      if (mounted) setReady(true);
    });
    return () => { mounted = false; unsubscribe(); stopWatching(); };
  }, [reset]);

  const signOut = useCallback(async () => {
    try {
      if (mode === 'authenticated') await identity.signOut();
    } finally {
      await reset('anonymous');
    }
  }, [mode, reset]);
  const signInWithGoogle = useCallback(async () => {
    const signedIn = await identity.signInWithGoogle();
    if (signedIn) await reset('authenticated');
    return signedIn;
  }, [reset]);
  const enterDemo = useCallback(() => { void reset('demo'); }, [reset]);

  return <Context.Provider value={{
    mode, ready, identityConfigured, enterDemo, signInWithGoogle,
    getAccessToken: identity.getAccessToken, signOut,
  }}>{children}</Context.Provider>;
}
export function useSession() {
  const value = useContext(Context);
  if (!value) throw new Error('Missing SessionProvider');
  return value;
}
