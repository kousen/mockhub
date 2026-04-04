import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import { useChat, useRecommendations, usePricePrediction } from './use-ai';

vi.mock('@/api/ai', () => ({
  sendChatMessage: vi.fn().mockResolvedValue({ message: 'Hello!' }),
  getRecommendations: vi.fn().mockResolvedValue({ events: [] }),
  getPricePrediction: vi.fn().mockResolvedValue({ prediction: 'stable' }),
}));

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: (selector: (state: { user: { id: number } | null }) => unknown) =>
    selector({ user: { id: 1 } }),
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

describe('use-ai hooks', () => {
  beforeEach(() => vi.clearAllMocks());

  it('useChat sends chat message', async () => {
    const aiApi = await import('@/api/ai');
    const { result } = renderHook(() => useChat(), { wrapper: createWrapper() });

    result.current.mutate({ message: 'Hello', conversationId: null });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(aiApi.sendChatMessage).toHaveBeenCalledWith({ message: 'Hello', conversationId: null });
  });

  it('useRecommendations fetches recommendations', async () => {
    const { result } = renderHook(() => useRecommendations(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ events: [] });
  });

  it('useRecommendations passes city parameter', async () => {
    const aiApi = await import('@/api/ai');
    const { result } = renderHook(() => useRecommendations('New York'), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(aiApi.getRecommendations).toHaveBeenCalledWith('New York');
  });

  it('usePricePrediction fetches prediction', async () => {
    const { result } = renderHook(() => usePricePrediction('test-event'), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ prediction: 'stable' });
  });

  it('usePricePrediction is disabled for empty slug', () => {
    const { result } = renderHook(() => usePricePrediction(''), { wrapper: createWrapper() });
    expect(result.current.fetchStatus).toBe('idle');
  });
});
