import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as notificationsApi from '@/api/notifications';
import { useAuthStore } from '@/stores/auth-store';
import type { Notification } from '@/types/notification';
import type { PageResponse } from '@/types/common';

/**
 * Hook for fetching paginated notifications.
 * Only enabled when authenticated.
 */
export function useNotifications(page?: number, size: number = 10) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return useQuery({
    queryKey: ['notifications', { page, size }],
    queryFn: () => notificationsApi.getNotifications(page, size),
    enabled: isAuthenticated,
  });
}

/**
 * Hook for fetching the count of unread notifications.
 * Polls every 30 seconds to keep the badge current.
 * Only enabled when authenticated.
 */
export function useUnreadCount() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => notificationsApi.getUnreadCount(),
    enabled: isAuthenticated,
    refetchInterval: 30 * 1000,
  });
}

/**
 * Hook for marking a single notification as read.
 * Optimistically updates both the notification list and unread count.
 */
export function useMarkAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: number) => notificationsApi.markAsRead(notificationId),
    onMutate: async (notificationId) => {
      await queryClient.cancelQueries({ queryKey: ['notifications'] });

      const previousCount = queryClient.getQueryData<number>(['notifications', 'unread-count']);
      if (previousCount !== undefined && previousCount > 0) {
        queryClient.setQueryData(['notifications', 'unread-count'], previousCount - 1);
      }

      // Update the notification in any cached page
      queryClient.setQueriesData<PageResponse<Notification>>(
        { queryKey: ['notifications'], exact: false },
        (old) => {
          if (!old || typeof old !== 'object' || !('content' in old)) return old;
          return {
            ...old,
            content: old.content.map((n) => (n.id === notificationId ? { ...n, isRead: true } : n)),
          };
        },
      );

      return { previousCount };
    },
    onError: (_error, _variables, context) => {
      if (context?.previousCount !== undefined) {
        queryClient.setQueryData(['notifications', 'unread-count'], context.previousCount);
      }
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications', 'unread-count'] });
    },
  });
}

/**
 * Hook for marking all notifications as read.
 * Optimistically sets unread count to zero and marks all cached notifications as read.
 */
export function useMarkAllAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificationsApi.markAllAsRead(),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['notifications'] });

      const previousCount = queryClient.getQueryData<number>(['notifications', 'unread-count']);
      queryClient.setQueryData(['notifications', 'unread-count'], 0);

      queryClient.setQueriesData<PageResponse<Notification>>(
        { queryKey: ['notifications'], exact: false },
        (old) => {
          if (!old || typeof old !== 'object' || !('content' in old)) return old;
          return {
            ...old,
            content: old.content.map((n) => ({ ...n, isRead: true })),
          };
        },
      );

      return { previousCount };
    },
    onError: (_error, _variables, context) => {
      if (context?.previousCount !== undefined) {
        queryClient.setQueryData(['notifications', 'unread-count'], context.previousCount);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });
}
