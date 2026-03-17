import { useQuery } from '@tanstack/react-query';
import * as eventsApi from '@/api/events';
import type { EventSearchParams } from '@/types/event';

/**
 * Hook for fetching a paginated list of events with optional filters.
 */
export function useEvents(params: EventSearchParams) {
  return useQuery({
    queryKey: ['events', params],
    queryFn: () => eventsApi.getEvents(params),
  });
}

/**
 * Hook for fetching featured events for the home page.
 * Uses a longer stale time since featured events change infrequently.
 */
export function useFeaturedEvents() {
  return useQuery({
    queryKey: ['events', 'featured'],
    queryFn: () => eventsApi.getFeaturedEvents(),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Hook for fetching a single event by its URL slug.
 */
export function useEvent(slug: string) {
  return useQuery({
    queryKey: ['events', slug],
    queryFn: () => eventsApi.getEvent(slug),
    enabled: slug.length > 0,
  });
}

/**
 * Hook for fetching active listings (tickets for sale) for an event.
 */
export function useEventListings(slug: string) {
  return useQuery({
    queryKey: ['events', slug, 'listings'],
    queryFn: () => eventsApi.getEventListings(slug),
    enabled: slug.length > 0,
  });
}

/**
 * Hook for fetching price history data for the price chart.
 */
export function useEventPriceHistory(slug: string) {
  return useQuery({
    queryKey: ['events', slug, 'price-history'],
    queryFn: () => eventsApi.getEventPriceHistory(slug),
    enabled: slug.length > 0,
  });
}

/**
 * Hook for fetching section availability for an event.
 */
export function useEventSections(slug: string) {
  return useQuery({
    queryKey: ['events', slug, 'sections'],
    queryFn: () => eventsApi.getEventSections(slug),
    enabled: slug.length > 0,
  });
}

/**
 * Hook for fetching all event categories.
 * Uses a long stale time since categories rarely change.
 */
export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => eventsApi.getCategories(),
    staleTime: 60 * 60 * 1000, // 1 hour
  });
}

/**
 * Hook for fetching all tags.
 * Uses a long stale time since tags rarely change.
 */
export function useTags() {
  return useQuery({
    queryKey: ['tags'],
    queryFn: () => eventsApi.getTags(),
    staleTime: 60 * 60 * 1000, // 1 hour
  });
}
