import { describe, it, expect, vi } from 'vitest';
import {
  getMyListings,
  createListing,
  updateListingPrice,
  deactivateListing,
  getEarnings,
} from './seller';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock' }),
    post: vi.fn().mockResolvedValue({ data: 'mock' }),
    put: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  },
}));

describe('seller API', () => {
  it('getMyListings without filter', async () => {
    const client = (await import('./client')).default;
    await getMyListings();
    expect(client.get).toHaveBeenCalledWith('/my/listings', { params: undefined });
  });

  it('getMyListings with status filter', async () => {
    const client = (await import('./client')).default;
    await getMyListings('ACTIVE');
    expect(client.get).toHaveBeenCalledWith('/my/listings', { params: { status: 'ACTIVE' } });
  });

  it('createListing', async () => {
    const client = (await import('./client')).default;
    await createListing({
      eventSlug: 'e',
      sectionName: 'S',
      rowLabel: 'A',
      seatNumber: '1',
      price: 100,
    });
    expect(client.post).toHaveBeenCalledWith(
      '/listings',
      expect.objectContaining({ eventSlug: 'e' }),
    );
  });

  it('updateListingPrice', async () => {
    const client = (await import('./client')).default;
    await updateListingPrice(1, { price: 120 });
    expect(client.put).toHaveBeenCalledWith('/listings/1/price', { price: 120 });
  });

  it('deactivateListing', async () => {
    const client = (await import('./client')).default;
    await deactivateListing(5);
    expect(client.delete).toHaveBeenCalledWith('/listings/5');
  });

  it('getEarnings', async () => {
    const client = (await import('./client')).default;
    await getEarnings();
    expect(client.get).toHaveBeenCalledWith('/my/earnings');
  });
});
