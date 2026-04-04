import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import {
  useNotifications,
  useUnreadCount,
  useMarkAsRead,
  useMarkAllAsRead,
} from './use-notifications';
import { useAuthStore } from '@/stores/auth-store';
import type { Notification } from '@/types/notification';
import type { PageResponse } from '@/types/common';

const mockNotifications: PageResponse<Notification> = {
  content: [
    {
      id: 1,
      type: 'ORDER_CONFIRMED',
      title: 'Order Confirmed',
      message: 'Your order has been confirmed',
      link: '/orders/ORD-001',
      isRead: false,
      createdAt: '2026-04-01T10:00:00Z',
    },
    {
      id: 2,
      type: 'PRICE_DROP',
      title: 'Price Drop',
      message: 'A ticket you favorited dropped in price',
      link: '/events/concert-a',
      isRead: false,
      createdAt: '2026-04-01T09:00:00Z',
    },
    {
      id: 3,
      type: 'SYSTEM',
      title: 'Welcome',
      message: 'Welcome to MockHub!',
      link: null,
      isRead: true,
      createdAt: '2026-03-31T10:00:00Z',
    },
  ],
  totalElements: 3,
  totalPages: 1,
  size: 10,
  number: 0,
  first: true,
  last: true,
};

vi.mock('@/api/notifications', () => ({
  getNotifications: vi.fn(),
  getUnreadCount: vi.fn(),
  markAsRead: vi.fn(),
  markAllAsRead: vi.fn(),
}));

async function getNotifApi() {
  return await import('@/api/notifications');
}

async function setupDefaultMocks() {
  const notifApi = await getNotifApi();
  vi.mocked(notifApi.getNotifications).mockResolvedValue(mockNotifications);
  vi.mocked(notifApi.getUnreadCount).mockResolvedValue(2);
  vi.mocked(notifApi.markAsRead).mockResolvedValue(undefined);
  vi.mocked(notifApi.markAllAsRead).mockResolvedValue(undefined);
}

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

describe('useNotifications', () => {
  beforeEach(async () => {
    vi.clearAllMocks();
    await setupDefaultMocks();
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
  });

  it('fetches notifications when authenticated', async () => {
    const qc = createQueryClient();
    const { result } = renderHook(() => useNotifications(0, 10), { wrapper: createWrapper(qc) });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.content.length).toBe(3);
  });

  it('does not fetch when not authenticated', () => {
    useAuthStore.setState({ isAuthenticated: false, user: null, accessToken: null });
    const qc = createQueryClient();
    const { result } = renderHook(() => useNotifications(), { wrapper: createWrapper(qc) });

    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('useUnreadCount', () => {
  beforeEach(async () => {
    vi.clearAllMocks();
    await setupDefaultMocks();
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
  });

  it('fetches unread count', async () => {
    const qc = createQueryClient();
    const { result } = renderHook(() => useUnreadCount(), { wrapper: createWrapper(qc) });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBe(2);
  });

  it('does not fetch when not authenticated', () => {
    useAuthStore.setState({ isAuthenticated: false, user: null, accessToken: null });
    const qc = createQueryClient();
    const { result } = renderHook(() => useUnreadCount(), { wrapper: createWrapper(qc) });

    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('useMarkAsRead — optimistic updates', () => {
  let qc: QueryClient;
  let notifApi: Awaited<ReturnType<typeof getNotifApi>>;

  beforeEach(async () => {
    vi.clearAllMocks();
    await setupDefaultMocks();
    notifApi = await getNotifApi();
    qc = createQueryClient();
    // Pre-seed caches
    qc.setQueryData(['notifications', { page: undefined, size: 10 }], mockNotifications);
    qc.setQueryData(['notifications', 'unread-count'], 2);
  });

  it('calls markAsRead API with correct notification ID', async () => {
    const { result } = renderHook(() => useMarkAsRead(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate(1);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(notifApi.markAsRead).toHaveBeenCalledWith(1);
  });

  it('rolls back unread count on error', async () => {
    vi.mocked(notifApi.markAsRead).mockRejectedValue(new Error('Server error'));

    const { result } = renderHook(() => useMarkAsRead(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate(1);
    });

    await waitFor(() => expect(result.current.isError).toBe(true));

    // onError should restore the previous count
    const count = qc.getQueryData<number>(['notifications', 'unread-count']);
    expect(count).toBe(2);
  });

  it('does not decrement unread count below zero', async () => {
    qc.setQueryData(['notifications', 'unread-count'], 0);

    const { result } = renderHook(() => useMarkAsRead(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate(3);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    const count = qc.getQueryData<number>(['notifications', 'unread-count']);
    expect(count).toBeGreaterThanOrEqual(0);
  });
});

describe('useMarkAllAsRead — optimistic updates', () => {
  let qc: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    qc = createQueryClient();
    qc.setQueryData(['notifications', { page: undefined, size: 10 }], mockNotifications);
    qc.setQueryData(['notifications', 'unread-count'], 2);
  });

  it('optimistically sets unread count to zero', async () => {
    const notifApi = await import('@/api/notifications');
    let resolveMarkAll!: (value: void) => void;
    vi.mocked(notifApi.markAllAsRead).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveMarkAll = resolve;
        }),
    );

    const { result } = renderHook(() => useMarkAllAsRead(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate();
    });

    await waitFor(() => {
      const count = qc.getQueryData<number>(['notifications', 'unread-count']);
      expect(count).toBe(0);
    });

    act(() => {
      resolveMarkAll();
    });
  });

  it('optimistically marks all notifications as read in cache', async () => {
    const notifApi = await import('@/api/notifications');
    let resolveMarkAll!: (value: void) => void;
    vi.mocked(notifApi.markAllAsRead).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveMarkAll = resolve;
        }),
    );

    const { result } = renderHook(() => useMarkAllAsRead(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate();
    });

    await waitFor(() => {
      const cached = qc.getQueryData<PageResponse<Notification>>([
        'notifications',
        { page: undefined, size: 10 },
      ]);
      const allRead = cached?.content.every((n) => n.isRead);
      expect(allRead).toBe(true);
    });

    act(() => {
      resolveMarkAll();
    });
  });

  it('rolls back unread count on error', async () => {
    const notifApi = await import('@/api/notifications');
    vi.mocked(notifApi.markAllAsRead).mockRejectedValueOnce(new Error('Server error'));

    const { result } = renderHook(() => useMarkAllAsRead(), { wrapper: createWrapper(qc) });

    act(() => {
      result.current.mutate();
    });

    await waitFor(() => expect(result.current.isError).toBe(true));

    // Should be rolled back to original count
    const count = qc.getQueryData<number>(['notifications', 'unread-count']);
    expect(count).toBe(2);
  });
});
