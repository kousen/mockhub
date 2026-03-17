import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as favoritesApi from '@/api/favorites';
import { useAuthStore } from '@/stores/auth-store';
import type { Favorite } from '@/types/favorite';

/**
 * Hook for fetching the current user's list of favorited events.
 * Only enabled when authenticated.
 */
export function useFavorites() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return useQuery({
    queryKey: ['favorites'],
    queryFn: () => favoritesApi.getFavorites(),
    enabled: isAuthenticated,
  });
}

/**
 * Hook for checking whether a specific event is favorited.
 * Only enabled when authenticated.
 */
export function useCheckFavorite(eventId: number) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return useQuery({
    queryKey: ['favorites', 'check', eventId],
    queryFn: () => favoritesApi.checkFavorite(eventId),
    enabled: isAuthenticated && eventId > 0,
  });
}

/**
 * Hook for adding an event to favorites.
 * Uses optimistic updates for the check query and invalidates the favorites list.
 */
export function useAddFavorite() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (eventId: number) => favoritesApi.addFavorite(eventId),
    onMutate: async (eventId) => {
      await queryClient.cancelQueries({
        queryKey: ['favorites', 'check', eventId],
      });
      const previousValue = queryClient.getQueryData<boolean>([
        'favorites',
        'check',
        eventId,
      ]);
      queryClient.setQueryData(['favorites', 'check', eventId], true);
      return { previousValue, eventId };
    },
    onError: (_error, _variables, context) => {
      if (context) {
        queryClient.setQueryData(
          ['favorites', 'check', context.eventId],
          context.previousValue,
        );
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
    },
  });
}

/**
 * Hook for removing an event from favorites.
 * Uses optimistic updates for the check query and invalidates the favorites list.
 */
export function useRemoveFavorite() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (eventId: number) => favoritesApi.removeFavorite(eventId),
    onMutate: async (eventId) => {
      await queryClient.cancelQueries({
        queryKey: ['favorites', 'check', eventId],
      });
      const previousValue = queryClient.getQueryData<boolean>([
        'favorites',
        'check',
        eventId,
      ]);
      queryClient.setQueryData(['favorites', 'check', eventId], false);

      // Optimistically remove from favorites list
      const previousFavorites = queryClient.getQueryData<Favorite[]>([
        'favorites',
      ]);
      if (previousFavorites) {
        queryClient.setQueryData(
          ['favorites'],
          previousFavorites.filter((fav) => fav.eventId !== eventId),
        );
      }

      return { previousValue, previousFavorites, eventId };
    },
    onError: (_error, _variables, context) => {
      if (context) {
        queryClient.setQueryData(
          ['favorites', 'check', context.eventId],
          context.previousValue,
        );
        if (context.previousFavorites) {
          queryClient.setQueryData(['favorites'], context.previousFavorites);
        }
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
    },
  });
}
