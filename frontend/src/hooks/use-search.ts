import { useQuery } from '@tanstack/react-query';
import * as searchApi from '@/api/search';

/**
 * Hook for performing a full event search.
 * Only runs when the query string is non-empty.
 */
export function useSearch(q: string, page?: number) {
  return useQuery({
    queryKey: ['search', q, page],
    queryFn: () => searchApi.searchEvents(q, page),
    enabled: q.trim().length > 0,
  });
}

/**
 * Hook for fetching search autocomplete suggestions.
 * Only runs when the query has at least 2 characters.
 * Uses a short stale time for fresh suggestions.
 */
export function useSuggestions(q: string) {
  return useQuery({
    queryKey: ['suggestions', q],
    queryFn: () => searchApi.getSuggestions(q),
    enabled: q.trim().length >= 2,
    staleTime: 30 * 1000, // 30 seconds
  });
}
