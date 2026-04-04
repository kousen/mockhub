import { describe, it, expect, vi } from 'vitest';
import { checkout, getOrders, getOrder, downloadTicket, downloadCalendar } from './orders';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock' }),
    post: vi.fn().mockResolvedValue({ data: 'mock' }),
  },
}));

describe('orders API', () => {
  it('checkout posts to /orders/checkout', async () => {
    const client = (await import('./client')).default;
    await checkout({ paymentMethod: 'MOCK' });
    expect(client.post).toHaveBeenCalledWith('/orders/checkout', { paymentMethod: 'MOCK' });
  });

  it('getOrders fetches paginated orders', async () => {
    const client = (await import('./client')).default;
    await getOrders(0, 10);
    expect(client.get).toHaveBeenCalledWith('/orders', { params: { page: 0, size: 10 } });
  });

  it('getOrder fetches by order number', async () => {
    const client = (await import('./client')).default;
    await getOrder('ORD-001');
    expect(client.get).toHaveBeenCalledWith('/orders/ORD-001');
  });

  it('downloadTicket fetches blob', async () => {
    const client = (await import('./client')).default;
    await downloadTicket('ORD-001', 1);
    expect(client.get).toHaveBeenCalledWith('/orders/ORD-001/tickets/1/download', {
      responseType: 'blob',
    });
  });

  it('downloadCalendar fetches blob', async () => {
    const client = (await import('./client')).default;
    await downloadCalendar('ORD-001');
    expect(client.get).toHaveBeenCalledWith('/orders/ORD-001/calendar', { responseType: 'blob' });
  });
});
