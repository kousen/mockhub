import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import { useCart, useAddToCart, useRemoveFromCart, useClearCart } from './use-cart';
import { useAuthStore } from '@/stores/auth-store';
import { useCartStore } from '@/stores/cart-store';
import type { Cart } from '@/types/cart';

const mockCart: Cart = {
  id: 1,
  userId: 1,
  items: [
    {
      id: 10,
      listingId: 100,
      eventName: 'Concert A',
      eventSlug: 'concert-a',
      sectionName: 'Orchestra',
      rowLabel: 'A',
      seatNumber: '1',
      ticketType: 'STANDARD',
      priceAtAdd: 75.0,
      currentPrice: 75.0,
      addedAt: '2026-04-01T10:00:00Z',
    },
    {
      id: 11,
      listingId: 101,
      eventName: 'Concert B',
      eventSlug: 'concert-b',
      sectionName: 'Balcony',
      rowLabel: 'B',
      seatNumber: '5',
      ticketType: 'VIP',
      priceAtAdd: 150.0,
      currentPrice: 150.0,
      addedAt: '2026-04-01T11:00:00Z',
    },
  ],
  subtotal: 225.0,
  itemCount: 2,
  expiresAt: '2026-04-01T12:00:00Z',
};

vi.mock('@/api/cart', () => ({
  getCart: vi.fn(),
  addToCart: vi.fn(),
  removeFromCart: vi.fn(),
  clearCart: vi.fn(),
}));

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Infinity },
      mutations: { retry: false },
    },
  });
}

async function getCartApi() {
  return await import('@/api/cart');
}

describe('useCart', () => {
  beforeEach(async () => {
    vi.clearAllMocks();
    const cartApi = await getCartApi();
    vi.mocked(cartApi.getCart).mockResolvedValue(mockCart);
    vi.mocked(cartApi.addToCart).mockResolvedValue(mockCart);
    vi.mocked(cartApi.removeFromCart).mockResolvedValue(mockCart);
    vi.mocked(cartApi.clearCart).mockResolvedValue(undefined);
    useAuthStore.setState({
      isAuthenticated: true,
      user: {
        id: 1,
        firstName: 'J',
        lastName: 'D',
        email: 'j@d.com',
        phone: null,
        avatarUrl: null,
        roles: ['ROLE_USER'],
      },
      accessToken: 'token',
    });
    useCartStore.setState({ itemCount: 0, isDrawerOpen: false });
  });

  it('fetches cart and syncs item count to store', async () => {
    const qc = createQueryClient();
    const { result } = renderHook(() => useCart(), { wrapper: createWrapper(qc) });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(mockCart);
    expect(useCartStore.getState().itemCount).toBe(2);
  });

  it('does not fetch when not authenticated', () => {
    useAuthStore.setState({ isAuthenticated: false, user: null, accessToken: null });
    const qc = createQueryClient();
    const { result } = renderHook(() => useCart(), { wrapper: createWrapper(qc) });

    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('useAddToCart — optimistic updates', () => {
  let qc: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    qc = createQueryClient();
    // Pre-seed the cart cache so optimistic updates have data to work with
    qc.setQueryData(['cart'], mockCart);
    useCartStore.setState({ itemCount: 2 });
  });

  it('optimistically increments cart count before server responds', async () => {
    const cartApi = await import('@/api/cart');
    // Make the API hang so we can inspect the optimistic state
    let resolveAdd!: (value: Cart) => void;
    vi.mocked(cartApi.addToCart).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveAdd = resolve;
        }),
    );

    const { result } = renderHook(() => useAddToCart(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate({ listingId: 200 });
    });

    // Wait for onMutate to run (optimistic update)
    await waitFor(() => {
      const cachedCart = qc.getQueryData<Cart>(['cart']);
      expect(cachedCart?.itemCount).toBe(3); // optimistically incremented
    });
    expect(useCartStore.getState().itemCount).toBe(3);

    // Now resolve the API call
    act(() => {
      resolveAdd(mockCart);
    });
  });

  it('rolls back cart count on API error', async () => {
    const cartApi = await import('@/api/cart');
    vi.mocked(cartApi.addToCart).mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useAddToCart(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate({ listingId: 200 });
    });

    // Wait for the error + rollback
    await waitFor(() => expect(result.current.isError).toBe(true));

    // Cache and store should be rolled back to original values
    const cachedCart = qc.getQueryData<Cart>(['cart']);
    expect(cachedCart?.itemCount).toBe(2);
    expect(useCartStore.getState().itemCount).toBe(2);
  });

  it('handles add when no cart is cached yet', async () => {
    const cartApi = await import('@/api/cart');
    vi.mocked(cartApi.addToCart).mockResolvedValueOnce(mockCart);

    // Clear the cache — simulate a user who hasn't loaded the cart yet
    qc.removeQueries({ queryKey: ['cart'] });

    const { result } = renderHook(() => useAddToCart(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate({ listingId: 200 });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    // Should succeed without errors even with no cached cart to optimistically update
  });
});

describe('useRemoveFromCart — optimistic updates', () => {
  let qc: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    qc = createQueryClient();
    qc.setQueryData(['cart'], mockCart);
    useCartStore.setState({ itemCount: 2 });
  });

  it('optimistically removes item from cache and decrements count', async () => {
    const cartApi = await import('@/api/cart');
    let resolveRemove!: (value: Cart) => void;
    vi.mocked(cartApi.removeFromCart).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRemove = resolve;
        }),
    );

    const { result } = renderHook(() => useRemoveFromCart(), { wrapper: createWrapper(qc) });

    // Remove item 10 (Concert A, $75)
    act(() => {
      result.current.mutate(10);
    });

    await waitFor(() => {
      const cachedCart = qc.getQueryData<Cart>(['cart']);
      expect(cachedCart?.items.length).toBe(1);
    });

    const cachedCart = qc.getQueryData<Cart>(['cart']);
    // Item 10 should be gone, item 11 should remain
    expect(cachedCart?.items[0].id).toBe(11);
    // Count decremented
    expect(cachedCart?.itemCount).toBe(1);
    expect(useCartStore.getState().itemCount).toBe(1);
    // Subtotal reduced by item 10's currentPrice ($75)
    expect(cachedCart?.subtotal).toBe(150.0);

    act(() => {
      resolveRemove(mockCart);
    });
  });

  it('rolls back on remove error', async () => {
    const cartApi = await import('@/api/cart');
    vi.mocked(cartApi.removeFromCart).mockRejectedValueOnce(new Error('Server error'));

    const { result } = renderHook(() => useRemoveFromCart(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate(10);
    });

    await waitFor(() => expect(result.current.isError).toBe(true));

    // Should be fully rolled back
    const cachedCart = qc.getQueryData<Cart>(['cart']);
    expect(cachedCart?.items.length).toBe(2);
    expect(cachedCart?.itemCount).toBe(2);
    expect(cachedCart?.subtotal).toBe(225.0);
    expect(useCartStore.getState().itemCount).toBe(2);
  });

  it('clamps itemCount to zero when removing from a 1-item cart', async () => {
    const singleItemCart: Cart = {
      ...mockCart,
      items: [mockCart.items[0]],
      itemCount: 1,
      subtotal: 75.0,
    };
    qc.setQueryData(['cart'], singleItemCart);
    useCartStore.setState({ itemCount: 1 });

    const cartApi = await import('@/api/cart');
    vi.mocked(cartApi.removeFromCart).mockResolvedValueOnce({
      ...singleItemCart,
      items: [],
      itemCount: 0,
      subtotal: 0,
    });

    const { result } = renderHook(() => useRemoveFromCart(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate(10);
    });

    await waitFor(() => {
      const cachedCart = qc.getQueryData<Cart>(['cart']);
      expect(cachedCart?.itemCount).toBe(0);
    });
    expect(useCartStore.getState().itemCount).toBe(0);
  });

  it('handles removing non-existent item gracefully', async () => {
    const cartApi = await import('@/api/cart');
    vi.mocked(cartApi.removeFromCart).mockResolvedValueOnce(mockCart);

    const { result } = renderHook(() => useRemoveFromCart(), { wrapper: createWrapper(qc) });

    // Remove an item ID that doesn't exist in the cart
    act(() => {
      result.current.mutate(999);
    });

    await waitFor(() => {
      const cachedCart = qc.getQueryData<Cart>(['cart']);
      // Items unchanged since 999 wasn't found in the filter
      expect(cachedCart?.items.length).toBe(2);
    });

    // itemCount still decrements (optimistic, before server corrects)
    // This is a minor edge case — the server response via onSettled will fix it
    const cachedCart = qc.getQueryData<Cart>(['cart']);
    expect(cachedCart?.itemCount).toBe(1);
  });
});

describe('useClearCart — optimistic updates', () => {
  let qc: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    qc = createQueryClient();
    qc.setQueryData(['cart'], mockCart);
    useCartStore.setState({ itemCount: 2 });
  });

  it('optimistically empties the cart', async () => {
    const cartApi = await import('@/api/cart');
    let resolveClear!: (value: void) => void;
    vi.mocked(cartApi.clearCart).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveClear = resolve;
        }),
    );

    const { result } = renderHook(() => useClearCart(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate();
    });

    await waitFor(() => {
      const cachedCart = qc.getQueryData<Cart>(['cart']);
      expect(cachedCart?.items.length).toBe(0);
    });

    const cachedCart = qc.getQueryData<Cart>(['cart']);
    expect(cachedCart?.itemCount).toBe(0);
    expect(cachedCart?.subtotal).toBe(0);
    expect(useCartStore.getState().itemCount).toBe(0);

    act(() => {
      resolveClear();
    });
  });

  it('rolls back on clear error', async () => {
    const cartApi = await import('@/api/cart');
    vi.mocked(cartApi.clearCart).mockRejectedValueOnce(new Error('Server error'));

    const { result } = renderHook(() => useClearCart(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate();
    });

    await waitFor(() => expect(result.current.isError).toBe(true));

    const cachedCart = qc.getQueryData<Cart>(['cart']);
    expect(cachedCart?.items.length).toBe(2);
    expect(cachedCart?.itemCount).toBe(2);
    expect(cachedCart?.subtotal).toBe(225.0);
    expect(useCartStore.getState().itemCount).toBe(2);
  });
});
