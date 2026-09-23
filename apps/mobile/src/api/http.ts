export class ApiError extends Error {
  constructor(public readonly status: number, public readonly code: string, message: string) { super(message); }
}
const messages: Record<number, string> = {
  401: 'Tu sesión ha caducado. Vuelve a iniciar sesión.',
  403: 'No tienes permiso para esta acción.',
  404: 'Este contenido ya no está disponible.',
  409: 'La acción entra en conflicto con el estado actual.',
  429: 'Demasiadas solicitudes. Inténtalo más tarde.',
  500: 'El servidor no está disponible. Inténtalo de nuevo.',
};
type ClientOptions = { baseUrl: string; getToken: () => Promise<string | null>; onUnauthorized: () => Promise<void> };
export function createHttpClient({ baseUrl, getToken, onUnauthorized }: ClientOptions) {
  return async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const token = await getToken();
    const headers = new Headers(init.headers);
    headers.set('Accept', 'application/json');
    if (init.body) headers.set('Content-Type', 'application/json');
    if (token) headers.set('Authorization', `Bearer ${token}`);
    // RN's AbortSignal polyfill does not implement AbortSignal.timeout().
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 15000);
    let response: Response;
    try {
      response = await fetch(`${baseUrl.replace(/\/$/, '')}${path}`, { ...init, headers, signal: init.signal ?? controller.signal });
    } catch { throw new ApiError(0, 'NETWORK_ERROR', 'No se ha podido conectar. Comprueba tu conexión.'); }
    finally { clearTimeout(timeout); }
    if (!response.ok) {
      if (response.status === 401) await onUnauthorized();
      // Do not show arbitrary server/proxy bodies or log credentials.
      throw new ApiError(response.status, `HTTP_${response.status}`, messages[response.status] ?? 'No se ha podido completar la acción.');
    }
    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  };
}
