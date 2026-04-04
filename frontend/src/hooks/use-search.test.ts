import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import { useSearch, useSuggestions } from './use-search';

vi.mock('@/api/search', () => ({
  searchEvents: vi.fn().mockResolvedValue({ content: [], totalElements: 0 }),
  getSuggestions: vi.fn().mockResolvedValue(['suggestion1', 'suggestion2']),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe('use-search hooks', () => {
  beforeEach(() => vi.clearAllMocks());

  it('useSearch fetches results for non-empty query', async () => {
    const { result } = renderHook(() => useSearch('rock concert'), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ content: [], totalElements: 0 });
  });

  it('useSearch is disabled for empty query', () => {
    const { result } = renderHook(() => useSearch(''), { wrapper: createWrapper() });
    expect(result.current.fetchStatus).toBe('idle');
  });

  it('useSearch is disabled for whitespace-only query', () => {
    const { result } = renderHook(() => useSearch('   '), { wrapper: createWrapper() });
    expect(result.current.fetchStatus).toBe('idle');
  });

  it('useSuggestions fetches for 2+ character query', async () => {
    const { result } = renderHook(() => useSuggestions('ro'), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(['suggestion1', 'suggestion2']);
  });

  it('useSuggestions is disabled for short query', () => {
    const { result } = renderHook(() => useSuggestions('r'), { wrapper: createWrapper() });
    expect(result.current.fetchStatus).toBe('idle');
  });
});
