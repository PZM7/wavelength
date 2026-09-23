import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, createHttpClient } from './http';

afterEach(() => vi.unstubAllGlobals());
describe('HTTP boundary', () => {
  it('sends bearer token and preserves request options', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('{"id":"one"}', { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);
    const request = createHttpClient({ baseUrl: 'https://api.example/', getToken: async () => 'test-token', onUnauthorized: async () => {} });
    expect(await request('/api/v1/me', { method: 'PATCH', body: '{}' })).toEqual({ id: 'one' });
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('https://api.example/api/v1/me');
    expect(new Headers(init.headers).get('Authorization')).toBe('Bearer test-token');
    expect(new Headers(init.headers).get('Content-Type')).toBe('application/json');
  });
  it.each([401, 403, 404, 429, 500])('maps %i without exposing server internals', async status => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('secret internal response', { status })));
    const onUnauthorized = vi.fn(async () => {});
    const request = createHttpClient({ baseUrl: 'https://api.example', getToken: async () => null, onUnauthorized });
    try { await request('/api/v1/me'); expect.fail('Expected error'); }
    catch (error) {
      expect(error).toBeInstanceOf(ApiError);
      expect((error as ApiError).status).toBe(status);
      expect((error as Error).message).not.toContain('secret');
    }
    expect(onUnauthorized).toHaveBeenCalledTimes(status === 401 ? 1 : 0);
  });
  it('accepts no-content responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })));
    const request = createHttpClient({ baseUrl: '', getToken: async () => null, onUnauthorized: async () => {} });
    expect(await request('/block', { method: 'POST' })).toBeUndefined();
  });
  it('normalizes network failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('network details')));
    const request = createHttpClient({ baseUrl: '', getToken: async () => null, onUnauthorized: async () => {} });
    await expect(request('/me')).rejects.toMatchObject({ status: 0, code: 'NETWORK_ERROR' });
  });
});
