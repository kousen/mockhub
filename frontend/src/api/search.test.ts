import { describe, it, expect, vi } from 'vitest';
import { searchEvents, getSuggestions } from './search';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock' }),
  },
}));

describe('search API', () => {
  it('searchEvents calls /events/search with query', async () => {
    const client = (await import('./client')).default;
    await searchEvents('rock', 0, 20);
    expect(client.get).toHaveBeenCalledWith('/events/search', {
      params: { q: 'rock', page: 0, size: 20 },
    });
  });

  it('getSuggestions calls /events/suggestions', async () => {
    const client = (await import('./client')).default;
    await getSuggestions('ro');
    expect(client.get).toHaveBeenCalledWith('/events/suggestions', { params: { q: 'ro' } });
  });
});
