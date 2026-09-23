import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const KEY = 'wavelength.access-token';
let webToken: string | null = null;
// Web preview is memory-only: never localStorage/sessionStorage.
export const tokenStore = {
  async get(): Promise<string | null> { return Platform.OS === 'web' ? webToken : SecureStore.getItemAsync(KEY); },
  async set(token: string): Promise<void> {
    if (Platform.OS === 'web') webToken = token;
    else await SecureStore.setItemAsync(KEY, token, { keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY });
  },
  async clear(): Promise<void> { webToken = null; if (Platform.OS !== 'web') await SecureStore.deleteItemAsync(KEY); },
};
