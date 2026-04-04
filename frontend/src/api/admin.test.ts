import { describe, it, expect, vi } from 'vitest';
import {
  getDashboardStats,
  getAdminEvents,
  getAdminEvent,
  createEvent,
  updateEvent,
  deleteEvent,
  getAdminUsers,
  updateUserRoles,
  updateUserStatus,
  getAdminOrders,
  generateTickets,
} from './admin';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock' }),
    post: vi.fn().mockResolvedValue({ data: 'mock' }),
    put: vi.fn().mockResolvedValue({ data: 'mock' }),
    patch: vi.fn().mockResolvedValue({ data: 'mock' }),
    delete: vi.fn().mockResolvedValue({}),
  },
}));

describe('admin API', () => {
  it('getDashboardStats', async () => {
    const client = (await import('./client')).default;
    await getDashboardStats();
    expect(client.get).toHaveBeenCalledWith('/admin/dashboard/stats');
  });

  it('getAdminEvents', async () => {
    const client = (await import('./client')).default;
    await getAdminEvents(0, 20);
    expect(client.get).toHaveBeenCalledWith('/admin/events', { params: { page: 0, size: 20 } });
  });

  it('getAdminEvent', async () => {
    const client = (await import('./client')).default;
    await getAdminEvent(1);
    expect(client.get).toHaveBeenCalledWith('/admin/events/1');
  });

  it('createEvent', async () => {
    const client = (await import('./client')).default;
    await createEvent({ name: 'Test' } as Parameters<typeof createEvent>[0]);
    expect(client.post).toHaveBeenCalledWith('/admin/events', { name: 'Test' });
  });

  it('updateEvent', async () => {
    const client = (await import('./client')).default;
    await updateEvent(1, { name: 'Updated' });
    expect(client.put).toHaveBeenCalledWith('/admin/events/1', { name: 'Updated' });
  });

  it('deleteEvent', async () => {
    const client = (await import('./client')).default;
    await deleteEvent(1);
    expect(client.delete).toHaveBeenCalledWith('/admin/events/1');
  });

  it('getAdminUsers', async () => {
    const client = (await import('./client')).default;
    await getAdminUsers(0, 20);
    expect(client.get).toHaveBeenCalledWith('/admin/users', { params: { page: 0, size: 20 } });
  });

  it('updateUserRoles', async () => {
    const client = (await import('./client')).default;
    await updateUserRoles({ userId: 1, roles: ['ROLE_ADMIN'] });
    expect(client.put).toHaveBeenCalledWith('/admin/users/1/roles', { roles: ['ROLE_ADMIN'] });
  });

  it('updateUserStatus', async () => {
    const client = (await import('./client')).default;
    await updateUserStatus({ userId: 1, enabled: false });
    expect(client.patch).toHaveBeenCalledWith('/admin/users/1/status', { enabled: false });
  });

  it('getAdminOrders', async () => {
    const client = (await import('./client')).default;
    await getAdminOrders(0, 20);
    expect(client.get).toHaveBeenCalledWith('/admin/orders', { params: { page: 0, size: 20 } });
  });

  it('generateTickets', async () => {
    const client = (await import('./client')).default;
    await generateTickets({
      eventId: 1,
      sectionName: 'Orchestra',
      rowCount: 10,
      seatsPerRow: 20,
      ticketType: 'STANDARD',
      basePrice: 50,
    });
    expect(client.post).toHaveBeenCalledWith('/admin/tickets/generate', {
      eventId: 1,
      sectionName: 'Orchestra',
      rowCount: 10,
      seatsPerRow: 20,
      ticketType: 'STANDARD',
      basePrice: 50,
    });
  });
});
