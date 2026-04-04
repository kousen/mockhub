import { describe, it, expect, vi } from 'vitest';
import { getSpotifyArtist, getSpotifyConnection, disconnectSpotify } from './spotify';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock' }),
    delete: vi.fn().mockResolvedValue({}),
  },
}));

vi.mock('axios', () => ({
  isAxiosError: vi.fn().mockReturnValue(false),
}));

describe('spotify API', () => {
  it('getSpotifyArtist calls correct endpoint', async () => {
    const client = (await import('./client')).default;
    await getSpotifyArtist('abc123');
    expect(client.get).toHaveBeenCalledWith('/api/v1/spotify/artists/abc123');
  });

  it('getSpotifyConnection calls /spotify/connection', async () => {
    const client = (await import('./client')).default;
    const result = await getSpotifyConnection();
    expect(client.get).toHaveBeenCalledWith('/spotify/connection');
    expect(result).toBe('mock');
  });

  it('getSpotifyConnection returns default on 401', async () => {
    const client = (await import('./client')).default;
    const { isAxiosError } = await import('axios');
    vi.mocked(isAxiosError).mockReturnValue(true);
    vi.mocked(client.get).mockRejectedValueOnce({ response: { status: 401 } });

    const result = await getSpotifyConnection();
    expect(result).toEqual({
      connected: false,
      scopeUpgradeNeeded: false,
      spotifyDisplayName: null,
      connectedAt: null,
    });

    vi.mocked(isAxiosError).mockReturnValue(false);
  });

  it('disconnectSpotify calls delete', async () => {
    const client = (await import('./client')).default;
    await disconnectSpotify();
    expect(client.delete).toHaveBeenCalledWith('/spotify/connection');
  });
});
