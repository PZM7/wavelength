import { describe, expect, it, vi } from 'vitest';

vi.mock('expo-secure-store', () => ({
  WHEN_UNLOCKED_THIS_DEVICE_ONLY: 1,
  getItemAsync: vi.fn(),
  setItemAsync: vi.fn(),
  deleteItemAsync: vi.fn(),
}));
vi.mock('react-native', () => ({ Platform: { OS: 'web' } }));

import { createChunkedStorage } from './sessionStorage';

function memory() {
  const values = new Map<string, string>();
  return {
    values,
    getItem: async (key: string) => values.get(key) ?? null,
    setItem: async (key: string, value: string) => { values.set(key, value); },
    removeItem: async (key: string) => { values.delete(key); },
  };
}

describe('secure session storage', () => {
  it('preserves large Unicode sessions in bounded chunks and clears old generations', async () => {
    const backing = memory();
    const storage = createChunkedStorage(backing);
    const large = '🎵'.repeat(2000) + 'fin';
    await storage.setItem('session', large);
    expect(await storage.getItem('session')).toBe(large);
    for (const value of backing.values.values()) expect(new TextEncoder().encode(value).length).toBeLessThanOrEqual(1600);
    await storage.setItem('session', 'new token');
    expect(await storage.getItem('session')).toBe('new token');
    expect(backing.values.size).toBe(2);
    await storage.removeItem('session');
    expect(await storage.getItem('session')).toBeNull();
    expect(backing.values.size).toBe(0);
  });

  it('retains the previous session if writing a replacement fails', async () => {
    const backing = memory();
    const storage = createChunkedStorage(backing);
    await storage.setItem('session', 'previous');
    const failing = createChunkedStorage({
      getItem: backing.getItem,
      setItem: async (key, value) => {
        if (key.endsWith('.1')) throw new Error('SecureStore unavailable');
        await backing.setItem(key, value);
      },
      removeItem: backing.removeItem,
    });
    await expect(failing.setItem('session', '🎵'.repeat(500))).rejects.toThrow('SecureStore unavailable');
    expect(await storage.getItem('session')).toBe('previous');
  });
});
