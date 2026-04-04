import { describe, it, expect, vi } from 'vitest';
import { getPublicOrderView } from './public-tickets';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { orderNumber: 'ORD-001' } }),
  },
}));

describe('public-tickets API', () => {
  it('getPublicOrderView calls /tickets/view with token', async () => {
    const client = (await import('./client')).default;
    const result = await getPublicOrderView('test-token');
    expect(client.get).toHaveBeenCalledWith('/tickets/view', { params: { token: 'test-token' } });
    expect(result).toEqual({ orderNumber: 'ORD-001' });
  });
});
