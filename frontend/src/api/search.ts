import apiClient from './client';
import type { PageResponse } from '@/types/common';
import type { EventSummary } from '@/types/event';

export async function searchEvents(
  q: string,
  page?: number,
  size?: number,
): Promise<PageResponse<EventSummary>> {
  const response = await apiClient.get<PageResponse<EventSummary>>('/events/search', {
    params: { q, page, size },
  });
  return response.data;
}

export async function getSuggestions(q: string): Promise<string[]> {
  const response = await apiClient.get<string[]>('/events/suggestions', {
    params: { q },
  });
  return response.data;
}
