import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import {
  useMyListings,
  useCreateListing,
  useUpdatePrice,
  useDeactivateListing,
  useEarnings,
} from './use-seller';

vi.mock('@/api/seller', () => ({
  getMyListings: vi.fn().mockResolvedValue([]),
  createListing: vi.fn().mockResolvedValue({ id: 1 }),
  updateListingPrice: vi.fn().mockResolvedValue({ id: 1 }),
  deactivateListing: vi.fn().mockResolvedValue(undefined),
  getEarnings: vi.fn().mockResolvedValue({ totalEarnings: 500 }),
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

describe('use-seller hooks', () => {
  beforeEach(() => vi.clearAllMocks());

  it('useMyListings fetches listings', async () => {
    const { result } = renderHook(() => useMyListings(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });

  it('useMyListings fetches listings with status filter', async () => {
    const sellerApi = await import('@/api/seller');
    const { result } = renderHook(() => useMyListings('ACTIVE'), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(sellerApi.getMyListings).toHaveBeenCalledWith('ACTIVE');
  });

  it('useCreateListing calls createListing', async () => {
    const sellerApi = await import('@/api/seller');
    const { result } = renderHook(() => useCreateListing(), { wrapper: createWrapper() });

    result.current.mutate({
      eventSlug: 'test-event',
      sectionName: 'Orchestra',
      rowLabel: 'A',
      seatNumber: '1',
      price: 100,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(sellerApi.createListing).toHaveBeenCalled();
  });

  it('useUpdatePrice calls updateListingPrice', async () => {
    const sellerApi = await import('@/api/seller');
    const { result } = renderHook(() => useUpdatePrice(), { wrapper: createWrapper() });

    result.current.mutate({ id: 1, request: { price: 120 } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(sellerApi.updateListingPrice).toHaveBeenCalledWith(1, { price: 120 });
  });

  it('useDeactivateListing calls deactivateListing', async () => {
    const sellerApi = await import('@/api/seller');
    const { result } = renderHook(() => useDeactivateListing(), { wrapper: createWrapper() });

    result.current.mutate(5);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(sellerApi.deactivateListing).toHaveBeenCalledWith(5);
  });

  it('useEarnings fetches earnings', async () => {
    const { result } = renderHook(() => useEarnings(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ totalEarnings: 500 });
  });
});
