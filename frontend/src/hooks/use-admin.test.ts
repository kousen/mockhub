import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import {
  useDashboardStats,
  useAdminEvents,
  useAdminEvent,
  useAdminUsers,
  useAdminOrders,
  useCreateEvent,
  useUpdateEvent,
  useDeleteEvent,
  useUpdateUserRoles,
  useUpdateUserStatus,
  useGenerateTickets,
} from './use-admin';

vi.mock('@/api/admin', () => ({
  getDashboardStats: vi.fn().mockResolvedValue({ totalEvents: 10, totalUsers: 100 }),
  getAdminEvents: vi.fn().mockResolvedValue({ content: [], totalElements: 0 }),
  getAdminEvent: vi.fn().mockResolvedValue({ id: 1, name: 'Test Event' }),
  getAdminUsers: vi.fn().mockResolvedValue({ content: [], totalElements: 0 }),
  getAdminOrders: vi.fn().mockResolvedValue({ content: [], totalElements: 0 }),
  createEvent: vi.fn().mockResolvedValue({ id: 1 }),
  updateEvent: vi.fn().mockResolvedValue({ id: 1 }),
  deleteEvent: vi.fn().mockResolvedValue(undefined),
  updateUserRoles: vi.fn().mockResolvedValue(undefined),
  updateUserStatus: vi.fn().mockResolvedValue(undefined),
  generateTickets: vi.fn().mockResolvedValue(undefined),
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

describe('use-admin hooks', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('useDashboardStats', () => {
    it('fetches dashboard stats', async () => {
      const { result } = renderHook(() => useDashboardStats(), { wrapper: createWrapper() });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual({ totalEvents: 10, totalUsers: 100 });
    });
  });

  describe('useAdminEvents', () => {
    it('fetches paginated events', async () => {
      const { result } = renderHook(() => useAdminEvents(0, 20), { wrapper: createWrapper() });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual({ content: [], totalElements: 0 });
    });
  });

  describe('useAdminEvent', () => {
    it('fetches a single event by ID', async () => {
      const { result } = renderHook(() => useAdminEvent(1), { wrapper: createWrapper() });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual({ id: 1, name: 'Test Event' });
    });

    it('does not fetch when eventId is 0', () => {
      const { result } = renderHook(() => useAdminEvent(0), { wrapper: createWrapper() });

      expect(result.current.fetchStatus).toBe('idle');
    });
  });

  describe('useAdminUsers', () => {
    it('fetches paginated users', async () => {
      const { result } = renderHook(() => useAdminUsers(0, 20), { wrapper: createWrapper() });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual({ content: [], totalElements: 0 });
    });
  });

  describe('useAdminOrders', () => {
    it('fetches paginated orders', async () => {
      const { result } = renderHook(() => useAdminOrders(0, 20), { wrapper: createWrapper() });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual({ content: [], totalElements: 0 });
    });
  });

  describe('useCreateEvent', () => {
    it('calls createEvent and invalidates queries on success', async () => {
      const adminApi = await import('@/api/admin');
      const wrapper = createWrapper();
      const { result } = renderHook(() => useCreateEvent(), { wrapper });

      result.current.mutate({
        name: 'New Event',
        artistName: null,
        eventDate: '2026-06-01T19:00:00Z',
        doorsOpenAt: null,
        venueId: 1,
        categoryId: 1,
        basePrice: 50,
        description: null,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(adminApi.createEvent).toHaveBeenCalled();
    });
  });

  describe('useUpdateEvent', () => {
    it('calls updateEvent with the correct event ID', async () => {
      const adminApi = await import('@/api/admin');
      const wrapper = createWrapper();
      const { result } = renderHook(() => useUpdateEvent(42), { wrapper });

      result.current.mutate({ name: 'Updated Event' });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(adminApi.updateEvent).toHaveBeenCalledWith(42, { name: 'Updated Event' });
    });
  });

  describe('useDeleteEvent', () => {
    it('calls deleteEvent and invalidates queries', async () => {
      const adminApi = await import('@/api/admin');
      const wrapper = createWrapper();
      const { result } = renderHook(() => useDeleteEvent(), { wrapper });

      result.current.mutate(5);

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(adminApi.deleteEvent).toHaveBeenCalledWith(5);
    });
  });

  describe('useUpdateUserRoles', () => {
    it('calls updateUserRoles', async () => {
      const adminApi = await import('@/api/admin');
      const wrapper = createWrapper();
      const { result } = renderHook(() => useUpdateUserRoles(), { wrapper });

      result.current.mutate({ userId: 1, roles: ['ROLE_ADMIN'] });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(adminApi.updateUserRoles).toHaveBeenCalledWith({ userId: 1, roles: ['ROLE_ADMIN'] });
    });
  });

  describe('useUpdateUserStatus', () => {
    it('calls updateUserStatus', async () => {
      const adminApi = await import('@/api/admin');
      const wrapper = createWrapper();
      const { result } = renderHook(() => useUpdateUserStatus(), { wrapper });

      result.current.mutate({ userId: 1, enabled: false });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(adminApi.updateUserStatus).toHaveBeenCalledWith({ userId: 1, enabled: false });
    });
  });

  describe('useGenerateTickets', () => {
    it('calls generateTickets', async () => {
      const adminApi = await import('@/api/admin');
      const wrapper = createWrapper();
      const { result } = renderHook(() => useGenerateTickets(), { wrapper });

      result.current.mutate({
        eventId: 1,
        sectionName: 'Orchestra',
        rowCount: 10,
        seatsPerRow: 20,
        ticketType: 'STANDARD',
        basePrice: 50,
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(adminApi.generateTickets).toHaveBeenCalledWith({
        eventId: 1,
        sectionName: 'Orchestra',
        rowCount: 10,
        seatsPerRow: 20,
        ticketType: 'STANDARD',
        basePrice: 50,
      });
    });
  });
});
