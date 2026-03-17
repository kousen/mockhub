import apiClient from './client';
import type { PageResponse } from '@/types/common';
import type { VenueSummary, VenueDetail } from '@/types/venue';

export async function getVenues(page?: number, size?: number): Promise<PageResponse<VenueSummary>> {
  const response = await apiClient.get<PageResponse<VenueSummary>>('/venues', {
    params: { page, size },
  });
  return response.data;
}

export async function getVenue(slug: string): Promise<VenueDetail> {
  const response = await apiClient.get<VenueDetail>(`/venues/${slug}`);
  return response.data;
}
