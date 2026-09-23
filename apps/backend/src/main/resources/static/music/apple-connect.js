const root = document.getElementById('connect');
const button = document.getElementById('authorize');
const message = document.getElementById('message');

button.addEventListener('click', async () => {
  button.disabled = true;
  message.textContent = 'Abriendo Apple Music…';
  try {
    if (!window.MusicKit) throw new Error('MusicKit no está disponible.');
    await window.MusicKit.configure({
      developerToken: root.dataset.token,
      app: { name: 'Wavelength', build: '0.1.0' },
    });
    const musicUserToken = await window.MusicKit.getInstance().authorize();
    if (!musicUserToken) throw new Error('Apple Music no devolvió una autorización.');
    const response = await fetch('/api/v1/music/apple/complete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ state: root.dataset.state, musicUserToken }),
      cache: 'no-store',
    });
    if (!response.ok) throw new Error('No se pudo completar la conexión. Inicia el proceso de nuevo.');
    const result = await response.json();
    window.location.replace(result.returnUri);
  } catch (error) {
    message.textContent = error instanceof Error ? error.message : 'No se pudo conectar Apple Music.';
    button.disabled = false;
  }
});
