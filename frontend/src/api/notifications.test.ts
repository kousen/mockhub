import { describe, it, expect, vi } from 'vitest';
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead } from './notifications';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { count: 5 } }),
    put: vi.fn().mockResolvedValue({}),
  },
}));

describe('notifications API', () => {
  it('getNotifications calls /notifications with params', async () => {
    const client = (await import('./client')).default;
    vi.mocked(client.get).mockResolvedValueOnce({ data: { content: [], totalElements: 0 } });
    const result = await getNotifications(0, 10);
    expect(client.get).toHaveBeenCalledWith('/notifications', { params: { page: 0, size: 10 } });
    expect(result).toEqual({ content: [], totalElements: 0 });
  });

  it('getUnreadCount returns count number', async () => {
    const client = (await import('./client')).default;
    vi.mocked(client.get).mockResolvedValueOnce({ data: { count: 3 } });
    const result = await getUnreadCount();
    expect(client.get).toHaveBeenCalledWith('/notifications/unread-count');
    expect(result).toBe(3);
  });

  it('markAsRead puts to /notifications/:id/read', async () => {
    const client = (await import('./client')).default;
    await markAsRead(42);
    expect(client.put).toHaveBeenCalledWith('/notifications/42/read');
  });

  it('markAllAsRead puts to /notifications/read-all', async () => {
    const client = (await import('./client')).default;
    await markAllAsRead();
    expect(client.put).toHaveBeenCalledWith('/notifications/read-all');
  });
});
