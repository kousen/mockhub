import { describe, it, expect, vi } from 'vitest';
import { getVenues, getVenue } from './venues';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock' }),
  },
}));

describe('venues API', () => {
  it('getVenues fetches paginated venues', async () => {
    const client = (await import('./client')).default;
    await getVenues(0, 20);
    expect(client.get).toHaveBeenCalledWith('/venues', { params: { page: 0, size: 20 } });
  });

  it('getVenue fetches by slug', async () => {
    const client = (await import('./client')).default;
    await getVenue('madison-square-garden');
    expect(client.get).toHaveBeenCalledWith('/venues/madison-square-garden');
  });
});
