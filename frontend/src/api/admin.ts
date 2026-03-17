import apiClient from './client';
import type { PageResponse } from '@/types/common';
import type {
  AdminEvent,
  AdminOrderSummary,
  AdminUser,
  CreateEventRequest,
  DashboardStats,
  GenerateTicketsRequest,
  UpdateEventRequest,
  UserRoleUpdate,
  UserStatusUpdate,
} from '@/types/admin';

export async function getDashboardStats(): Promise<DashboardStats> {
  const response = await apiClient.get<DashboardStats>('/admin/dashboard/stats');
  return response.data;
}

export async function getAdminEvents(
  page?: number,
  size?: number,
): Promise<PageResponse<AdminEvent>> {
  const response = await apiClient.get<PageResponse<AdminEvent>>('/admin/events', {
    params: { page, size },
  });
  return response.data;
}

export async function getAdminEvent(eventId: number): Promise<AdminEvent> {
  const response = await apiClient.get<AdminEvent>(`/admin/events/${eventId}`);
  return response.data;
}

export async function createEvent(data: CreateEventRequest): Promise<AdminEvent> {
  const response = await apiClient.post<AdminEvent>('/admin/events', data);
  return response.data;
}

export async function updateEvent(eventId: number, data: UpdateEventRequest): Promise<AdminEvent> {
  const response = await apiClient.put<AdminEvent>(`/admin/events/${eventId}`, data);
  return response.data;
}

export async function deleteEvent(eventId: number): Promise<void> {
  await apiClient.delete(`/admin/events/${eventId}`);
}

export async function getAdminUsers(
  page?: number,
  size?: number,
): Promise<PageResponse<AdminUser>> {
  const response = await apiClient.get<PageResponse<AdminUser>>('/admin/users', {
    params: { page, size },
  });
  return response.data;
}

export async function updateUserRoles(data: UserRoleUpdate): Promise<AdminUser> {
  const response = await apiClient.put<AdminUser>(`/admin/users/${data.userId}/roles`, {
    roles: data.roles,
  });
  return response.data;
}

export async function updateUserStatus(data: UserStatusUpdate): Promise<AdminUser> {
  const response = await apiClient.patch<AdminUser>(`/admin/users/${data.userId}/status`, {
    enabled: data.enabled,
  });
  return response.data;
}

export async function getAdminOrders(
  page?: number,
  size?: number,
): Promise<PageResponse<AdminOrderSummary>> {
  const response = await apiClient.get<PageResponse<AdminOrderSummary>>('/admin/orders', {
    params: { page, size },
  });
  return response.data;
}

export async function generateTickets(data: GenerateTicketsRequest): Promise<void> {
  await apiClient.post('/admin/tickets/generate', data);
}
