import apiClient from './client';
import type { PageResponse } from '@/types/common';
import type {
  EventSummary,
  EventDetail,
  EventSearchParams,
  Category,
  Tag,
} from '@/types/event';
import type { Listing, PriceHistory, SectionAvailability } from '@/types/ticket';

export async function getEvents(
  params: EventSearchParams,
): Promise<PageResponse<EventSummary>> {
  const response = await apiClient.get<PageResponse<EventSummary>>('/events', { params });
  return response.data;
}

export async function getFeaturedEvents(): Promise<EventSummary[]> {
  const response = await apiClient.get<EventSummary[]>('/events/featured');
  return response.data;
}

export async function getEvent(slug: string): Promise<EventDetail> {
  const response = await apiClient.get<EventDetail>(`/events/${slug}`);
  return response.data;
}

export async function getEventListings(slug: string): Promise<Listing[]> {
  const response = await apiClient.get<Listing[]>(`/events/${slug}/listings`);
  return response.data;
}

export async function getEventPriceHistory(slug: string): Promise<PriceHistory[]> {
  const response = await apiClient.get<PriceHistory[]>(`/events/${slug}/price-history`);
  return response.data;
}

export async function getEventSections(slug: string): Promise<SectionAvailability[]> {
  const response = await apiClient.get<SectionAvailability[]>(`/events/${slug}/sections`);
  return response.data;
}

export async function getCategories(): Promise<Category[]> {
  const response = await apiClient.get<Category[]>('/categories');
  return response.data;
}

export async function getTags(): Promise<Tag[]> {
  const response = await apiClient.get<Tag[]>('/tags');
  return response.data;
}
