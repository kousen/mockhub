import { describe, it, expect, vi } from 'vitest';
import {
  getEvents,
  getFeaturedEvents,
  getEvent,
  getEventListings,
  getEventPriceHistory,
  getEventSections,
  getCategories,
  getTags,
} from './events';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock-data' }),
  },
}));

describe('events API', () => {
  it('getEvents calls /events with params', async () => {
    const client = (await import('./client')).default;
    const result = await getEvents({ page: 0, size: 10 });
    expect(client.get).toHaveBeenCalledWith('/events', { params: { page: 0, size: 10 } });
    expect(result).toBe('mock-data');
  });

  it('getFeaturedEvents calls /events/featured', async () => {
    const client = (await import('./client')).default;
    await getFeaturedEvents();
    expect(client.get).toHaveBeenCalledWith('/events/featured');
  });

  it('getEvent calls /events/:slug', async () => {
    const client = (await import('./client')).default;
    await getEvent('test-slug');
    expect(client.get).toHaveBeenCalledWith('/events/test-slug');
  });

  it('getEventListings calls /events/:slug/listings', async () => {
    const client = (await import('./client')).default;
    await getEventListings('test-slug');
    expect(client.get).toHaveBeenCalledWith('/events/test-slug/listings');
  });

  it('getEventPriceHistory calls /events/:slug/price-history', async () => {
    const client = (await import('./client')).default;
    await getEventPriceHistory('test-slug');
    expect(client.get).toHaveBeenCalledWith('/events/test-slug/price-history');
  });

  it('getEventSections calls /events/:slug/sections', async () => {
    const client = (await import('./client')).default;
    await getEventSections('test-slug');
    expect(client.get).toHaveBeenCalledWith('/events/test-slug/sections');
  });

  it('getCategories calls /categories', async () => {
    const client = (await import('./client')).default;
    await getCategories();
    expect(client.get).toHaveBeenCalledWith('/categories');
  });

  it('getTags calls /tags', async () => {
    const client = (await import('./client')).default;
    await getTags();
    expect(client.get).toHaveBeenCalledWith('/tags');
  });
});
