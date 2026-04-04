import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import { useCheckout, useOrders, useOrder, useDownloadTicket } from './use-orders';

vi.mock('@/api/orders', () => ({
  checkout: vi.fn().mockResolvedValue({ orderNumber: 'ORD-001' }),
  getOrders: vi.fn().mockResolvedValue({ content: [], totalElements: 0 }),
  getOrder: vi.fn().mockResolvedValue({ orderNumber: 'ORD-001', status: 'CONFIRMED' }),
  downloadTicket: vi.fn().mockResolvedValue(new Blob(['pdf'])),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
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

describe('use-orders hooks', () => {
  beforeEach(() => vi.clearAllMocks());

  it('useOrders fetches paginated orders', async () => {
    const { result } = renderHook(() => useOrders(0), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ content: [], totalElements: 0 });
  });

  it('useOrder fetches a single order', async () => {
    const { result } = renderHook(() => useOrder('ORD-001'), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ orderNumber: 'ORD-001', status: 'CONFIRMED' });
  });

  it('useOrder is disabled for empty order number', () => {
    const { result } = renderHook(() => useOrder(''), { wrapper: createWrapper() });
    expect(result.current.fetchStatus).toBe('idle');
  });

  it('useCheckout calls checkout API', async () => {
    const ordersApi = await import('@/api/orders');
    const { result } = renderHook(() => useCheckout(), { wrapper: createWrapper() });

    result.current.mutate({ paymentMethodId: 'pm_test' });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(ordersApi.checkout).toHaveBeenCalledWith({ paymentMethodId: 'pm_test' });
  });

  it('useDownloadTicket triggers download on success', async () => {
    const { result } = renderHook(() => useDownloadTicket(), { wrapper: createWrapper() });

    // Mock URL.createObjectURL and revokeObjectURL
    const mockUrl = 'blob:test';
    const createObjectURL = vi.fn().mockReturnValue(mockUrl);
    const revokeObjectURL = vi.fn();
    globalThis.URL.createObjectURL = createObjectURL;
    globalThis.URL.revokeObjectURL = revokeObjectURL;

    const mockClick = vi.fn();
    vi.spyOn(document, 'createElement').mockReturnValue({
      href: '',
      download: '',
      click: mockClick,
    } as unknown as HTMLAnchorElement);

    result.current.mutate({ orderNumber: 'ORD-001', ticketId: 1 });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(createObjectURL).toHaveBeenCalled();
    expect(mockClick).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith(mockUrl);

    vi.restoreAllMocks();
  });
});
