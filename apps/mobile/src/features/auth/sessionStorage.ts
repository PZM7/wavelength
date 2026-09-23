import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

type Storage = {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
  removeItem(key: string): Promise<void>;
};
type Manifest = { version: 1; generation: string; count: number };

// SecureStore can reject values above roughly 2 KB on older iOS versions.
// 400 Unicode code points are at most 1,600 UTF-8 bytes.
export function createChunkedStorage(storage: Storage, prefix = 'wavelength.auth'): Storage {
  let sequence = 0;
  const manifestKey = (key: string) => `${prefix}.${key}.manifest`;
  const chunkKey = (key: string, generation: string, index: number) => `${prefix}.${key}.${generation}.${index}`;
  async function manifest(key: string): Promise<Manifest | null> {
    const raw = await storage.getItem(manifestKey(key));
    if (!raw) return null;
    try {
      const value = JSON.parse(raw) as Manifest;
      return value.version === 1 && typeof value.generation === 'string' && Number.isInteger(value.count) && value.count > 0 && value.count <= 128 ? value : null;
    } catch { return null; }
  }
  async function clearChunks(key: string, value: Manifest | null) {
    if (!value) return;
    await Promise.all(Array.from({ length: value.count }, (_, index) => storage.removeItem(chunkKey(key, value.generation, index))));
  }
  return {
    async getItem(key) {
      const current = await manifest(key);
      if (!current) return null;
      const parts = await Promise.all(Array.from({ length: current.count }, (_, index) => storage.getItem(chunkKey(key, current.generation, index))));
      return parts.every(part => part !== null) ? parts.join('') : null;
    },
    async setItem(key, value) {
      const previous = await manifest(key);
      // Multiple adapter instances can write within the same millisecond.
      const generation = `${Date.now().toString(36)}-${++sequence}-${Math.random().toString(36).slice(2)}`;
      const parts: string[] = [];
      let part = '';
      let count = 0;
      for (const character of value) {
        if (count === 400) { parts.push(part); part = ''; count = 0; }
        part += character;
        count++;
      }
      parts.push(part);
      if (parts.length > 128) throw new Error('Session is too large to store securely');
      const next: Manifest = { version: 1, generation, count: parts.length };
      try {
        for (let index = 0; index < parts.length; index++) await storage.setItem(chunkKey(key, generation, index), parts[index]!);
        await storage.setItem(manifestKey(key), JSON.stringify(next));
      } catch (error) {
        await clearChunks(key, next);
        throw error;
      }
      try { await clearChunks(key, previous); } catch { /* New session is already committed. */ }
    },
    async removeItem(key) {
      const current = await manifest(key);
      await storage.removeItem(manifestKey(key));
      await clearChunks(key, current);
    },
  };
}

const webValues = new Map<string, string>();
const webStorage: Storage = {
  async getItem(key) { return webValues.get(key) ?? null; },
  async setItem(key, value) { webValues.set(key, value); },
  async removeItem(key) { webValues.delete(key); },
};
const nativeStorage = createChunkedStorage({
  getItem: key => SecureStore.getItemAsync(key),
  setItem: (key, value) => SecureStore.setItemAsync(key, value, { keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY }),
  removeItem: key => SecureStore.deleteItemAsync(key),
});

// The web preview keeps its session in memory only; native sessions survive restarts.
export const sessionStorage = Platform.OS === 'web' ? webStorage : nativeStorage;
