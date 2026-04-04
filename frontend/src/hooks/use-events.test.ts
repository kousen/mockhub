import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import {
  useEvents,
  useFeaturedEvents,
  useEvent,
  useEventListings,
  useEventPriceHistory,
  useEventSections,
  useCategories,
  useTags,
} from './use-events';

vi.mock('@/api/events', () => ({
  getEvents: vi.fn().mockResolvedValue({ content: [], totalElements: 0 }),
  getFeaturedEvents: vi.fn().mockResolvedValue([]),
  getEvent: vi.fn().mockResolvedValue({ id: 1, name: 'Test Event', slug: 'test-event' }),
  getEventListings: vi.fn().mockResolvedValue([]),
  getEventPriceHistory: vi.fn().mockResolvedValue([]),
  getEventSections: vi.fn().mockResolvedValue([]),
  getCategories: vi.fn().mockResolvedValue([{ id: 1, name: 'Rock' }]),
  getTags: vi.fn().mockResolvedValue([{ id: 1, name: 'live' }]),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe('use-events hooks', () => {
  beforeEach(() => vi.clearAllMocks());

  it('useEvents fetches events with params', async () => {
    const { result } = renderHook(() => useEvents({ page: 0, size: 10 }), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ content: [], totalElements: 0 });
  });

  it('useFeaturedEvents fetches featured events', async () => {
    const { result } = renderHook(() => useFeaturedEvents(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });

  it('useEvent fetches event by slug', async () => {
    const { result } = renderHook(() => useEvent('test-event'), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ id: 1, name: 'Test Event', slug: 'test-event' });
  });

  it('useEvent is disabled for empty slug', () => {
    const { result } = renderHook(() => useEvent(''), { wrapper: createWrapper() });
    expect(result.current.fetchStatus).toBe('idle');
  });

  it('useEventListings fetches listings for event', async () => {
    const { result } = renderHook(() => useEventListings('test-event'), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });

  it('useEventPriceHistory fetches price history', async () => {
    const { result } = renderHook(() => useEventPriceHistory('test-event'), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });

  it('useEventSections fetches sections', async () => {
    const { result } = renderHook(() => useEventSections('test-event'), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });

  it('useCategories fetches categories', async () => {
    const { result } = renderHook(() => useCategories(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([{ id: 1, name: 'Rock' }]);
  });

  it('useTags fetches tags', async () => {
    const { result } = renderHook(() => useTags(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([{ id: 1, name: 'live' }]);
  });
});
