import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as adminApi from '@/api/admin';
import type {
  CreateEventRequest,
  GenerateTicketsRequest,
  UpdateEventRequest,
  UserRoleUpdate,
  UserStatusUpdate,
} from '@/types/admin';

/**
 * Hook for fetching admin dashboard statistics.
 */
export function useDashboardStats() {
  return useQuery({
    queryKey: ['admin', 'stats'],
    queryFn: () => adminApi.getDashboardStats(),
    staleTime: 60 * 1000, // 1 minute
  });
}

/**
 * Hook for fetching a paginated list of all events (admin view).
 */
export function useAdminEvents(page?: number, size?: number) {
  return useQuery({
    queryKey: ['admin', 'events', { page, size }],
    queryFn: () => adminApi.getAdminEvents(page, size),
  });
}

/**
 * Hook for fetching a single event by ID (admin view).
 */
export function useAdminEvent(eventId: number) {
  return useQuery({
    queryKey: ['admin', 'events', eventId],
    queryFn: () => adminApi.getAdminEvent(eventId),
    enabled: eventId > 0,
  });
}

/**
 * Hook for creating a new event.
 */
export function useCreateEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateEventRequest) => adminApi.createEvent(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'events'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
    },
  });
}

/**
 * Hook for updating an existing event.
 */
export function useUpdateEvent(eventId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateEventRequest) => adminApi.updateEvent(eventId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'events'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
    },
  });
}

/**
 * Hook for deleting an event.
 */
export function useDeleteEvent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (eventId: number) => adminApi.deleteEvent(eventId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'events'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
    },
  });
}

/**
 * Hook for fetching a paginated list of all users (admin view).
 */
export function useAdminUsers(page?: number, size?: number) {
  return useQuery({
    queryKey: ['admin', 'users', { page, size }],
    queryFn: () => adminApi.getAdminUsers(page, size),
  });
}

/**
 * Hook for updating a user's roles.
 */
export function useUpdateUserRoles() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UserRoleUpdate) => adminApi.updateUserRoles(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
  });
}

/**
 * Hook for enabling/disabling a user account.
 */
export function useUpdateUserStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UserStatusUpdate) => adminApi.updateUserStatus(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
  });
}

/**
 * Hook for fetching a paginated list of all orders (admin view).
 */
export function useAdminOrders(page?: number, size?: number) {
  return useQuery({
    queryKey: ['admin', 'orders', { page, size }],
    queryFn: () => adminApi.getAdminOrders(page, size),
  });
}

/**
 * Hook for generating tickets for an event.
 */
export function useGenerateTickets() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: GenerateTicketsRequest) => adminApi.generateTickets(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'events'] });
    },
  });
}
