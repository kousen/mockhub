import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import { useSpotifyArtist, useSpotifyConnection, useDisconnectSpotify } from './use-spotify';

vi.mock('@/api/spotify', () => ({
  getSpotifyArtist: vi.fn().mockResolvedValue({ id: 'abc', name: 'Test Artist', genres: ['rock'] }),
  getSpotifyConnection: vi.fn().mockResolvedValue({ connected: true }),
  disconnectSpotify: vi.fn().mockResolvedValue(undefined),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe('use-spotify hooks', () => {
  beforeEach(() => vi.clearAllMocks());

  it('useSpotifyArtist fetches artist data', async () => {
    const { result } = renderHook(() => useSpotifyArtist('abc123'), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ id: 'abc', name: 'Test Artist', genres: ['rock'] });
  });

  it('useSpotifyArtist is disabled when id is null', () => {
    const { result } = renderHook(() => useSpotifyArtist(null), { wrapper: createWrapper() });
    expect(result.current.fetchStatus).toBe('idle');
  });

  it('useSpotifyConnection fetches connection status', async () => {
    const { result } = renderHook(() => useSpotifyConnection(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ connected: true });
  });

  it('useDisconnectSpotify calls disconnect API', async () => {
    const spotifyApi = await import('@/api/spotify');
    const { result } = renderHook(() => useDisconnectSpotify(), { wrapper: createWrapper() });

    result.current.mutate();

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(spotifyApi.disconnectSpotify).toHaveBeenCalled();
  });
});
