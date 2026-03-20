import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as sellerApi from '@/api/seller';
import type { SellListingRequest, UpdatePriceRequest } from '@/types/seller';

/**
 * Hook for fetching the current user's listings.
 * Optionally filters by status (ACTIVE, SOLD, CANCELLED).
 */
export function useMyListings(status?: string) {
  return useQuery({
    queryKey: ['my-listings', { status }],
    queryFn: () => sellerApi.getMyListings(status),
  });
}

/**
 * Hook for creating a new listing.
 * Invalidates the listings cache on success.
 */
export function useCreateListing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: SellListingRequest) => sellerApi.createListing(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-listings'] });
    },
  });
}

/**
 * Hook for updating the price of an existing listing.
 * Invalidates the listings cache on success.
 */
export function useUpdatePrice() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdatePriceRequest }) =>
      sellerApi.updateListingPrice(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-listings'] });
    },
  });
}

/**
 * Hook for deactivating (cancelling) a listing.
 * Invalidates the listings cache on success.
 */
export function useDeactivateListing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => sellerApi.deactivateListing(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-listings'] });
    },
  });
}

/**
 * Hook for fetching the seller's earnings summary.
 */
export function useEarnings() {
  return useQuery({
    queryKey: ['my-earnings'],
    queryFn: () => sellerApi.getEarnings(),
  });
}
