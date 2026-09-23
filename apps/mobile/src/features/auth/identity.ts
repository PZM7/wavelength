import { makeRedirectUri } from 'expo-auth-session';
import * as WebBrowser from 'expo-web-browser';
import { AppState, Platform } from 'react-native';
import { getSupabase, identityConfigured } from './supabase';

export { identityConfigured };

let refreshInFlight: Promise<string | null> | null = null;

export const identity = {
  subscribe(listener: (authenticated: boolean, clearQueries: boolean) => void): () => void {
    const client = getSupabase();
    if (!client) return () => {};
    const { data } = client.auth.onAuthStateChange((event, session) => {
      if (event === 'SIGNED_OUT') listener(false, true);
      if (event === 'SIGNED_IN') listener(Boolean(session), true);
      if (event === 'TOKEN_REFRESHED') listener(Boolean(session), false);
    });
    return () => data.subscription.unsubscribe();
  },
  watchAppState(): () => void {
    const client = getSupabase();
    if (!client || Platform.OS === 'web') return () => {};
    client.auth.startAutoRefresh();
    const subscription = AppState.addEventListener('change', state => {
      if (state === 'active') client.auth.startAutoRefresh();
      else client.auth.stopAutoRefresh();
    });
    return () => { subscription.remove(); client.auth.stopAutoRefresh(); };
  },
  async getAccessToken(forceRefresh = false): Promise<string | null> {
    const client = getSupabase();
    if (!client) return null;
    if (forceRefresh) {
      if (!refreshInFlight) {
        refreshInFlight = (async () => {
          const { data, error } = await client.auth.refreshSession();
          if (error) {
            if (error.status !== undefined && error.status >= 400 && error.status < 500) return null;
            throw new Error('No se ha podido renovar la sesión. Comprueba tu conexión.');
          }
          return data.session?.access_token ?? null;
        })().finally(() => { refreshInFlight = null; });
      }
      return refreshInFlight;
    }
    const { data, error } = await client.auth.getSession();
    if (error) {
      if (error.status !== undefined && error.status >= 400 && error.status < 500) return null;
      throw new Error('No se ha podido comprobar la sesión. Comprueba tu conexión.');
    }
    return data.session?.access_token ?? null;
  },
  async signInWithGoogle(): Promise<boolean> {
    const client = getSupabase();
    if (!client) throw new Error('Configura Supabase antes de iniciar sesión.');
    const redirectTo = makeRedirectUri({ scheme: 'wavelength', path: 'auth/callback' });
    const { data, error } = await client.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo, skipBrowserRedirect: true },
    });
    if (error || !data.url) throw new Error('No se ha podido iniciar el acceso con Google.');
    const result = await WebBrowser.openAuthSessionAsync(data.url, redirectTo);
    if (result.type !== 'success') return false;
    const callback = new URL(result.url);
    const code = callback.searchParams.get('code');
    if (!code) throw new Error('Google no devolvió un código de acceso válido.');
    const exchanged = await client.auth.exchangeCodeForSession(code);
    if (exchanged.error || !exchanged.data.session) throw new Error('No se ha podido completar el inicio de sesión.');
    return true;
  },
  async signOut(): Promise<void> {
    const client = getSupabase();
    if (!client) return;
    const { error } = await client.auth.signOut({ scope: 'local' });
    if (error) throw new Error('No se ha podido cerrar la sesión. Inténtalo de nuevo.');
  },
};
