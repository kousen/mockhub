import { describe, it, expect, vi } from 'vitest';
import { getFavorites, addFavorite, removeFavorite, checkFavorite } from './favorites';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { favorited: true } }),
    post: vi.fn().mockResolvedValue({ data: 'mock' }),
    delete: vi.fn().mockResolvedValue({}),
  },
}));

describe('favorites API', () => {
  it('getFavorites calls /favorites', async () => {
    const client = (await import('./client')).default;
    vi.mocked(client.get).mockResolvedValueOnce({ data: [] });
    const result = await getFavorites();
    expect(client.get).toHaveBeenCalledWith('/favorites');
    expect(result).toEqual([]);
  });

  it('addFavorite posts eventId', async () => {
    const client = (await import('./client')).default;
    await addFavorite(1);
    expect(client.post).toHaveBeenCalledWith('/favorites', { eventId: 1 });
  });

  it('removeFavorite deletes by eventId', async () => {
    const client = (await import('./client')).default;
    await removeFavorite(1);
    expect(client.delete).toHaveBeenCalledWith('/favorites/1');
  });

  it('checkFavorite returns boolean', async () => {
    const client = (await import('./client')).default;
    vi.mocked(client.get).mockResolvedValueOnce({ data: { favorited: true } });
    const result = await checkFavorite(1);
    expect(client.get).toHaveBeenCalledWith('/favorites/check/1');
    expect(result).toBe(true);
  });
});
