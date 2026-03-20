import apiClient from './client';
import type {
  SellerListing,
  SellListingRequest,
  UpdatePriceRequest,
  EarningsSummary,
} from '@/types/seller';

export async function getMyListings(status?: string): Promise<SellerListing[]> {
  const response = await apiClient.get<SellerListing[]>('/my/listings', {
    params: status ? { status } : undefined,
  });
  return response.data;
}

export async function createListing(request: SellListingRequest): Promise<SellerListing> {
  const response = await apiClient.post<SellerListing>('/listings', request);
  return response.data;
}

export async function updateListingPrice(
  id: number,
  request: UpdatePriceRequest,
): Promise<void> {
  await apiClient.put(`/listings/${id}/price`, request);
}

export async function deactivateListing(id: number): Promise<void> {
  await apiClient.delete(`/listings/${id}`);
}

export async function getEarnings(): Promise<EarningsSummary> {
  const response = await apiClient.get<EarningsSummary>('/my/earnings');
  return response.data;
}
