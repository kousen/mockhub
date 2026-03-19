import apiClient from './client';
import type { PageResponse } from '@/types/common';
import type { Notification, UnreadCountResponse } from '@/types/notification';

export async function getNotifications(
  page?: number,
  size?: number,
): Promise<PageResponse<Notification>> {
  const response = await apiClient.get<PageResponse<Notification>>('/notifications', {
    params: { page, size },
  });
  return response.data;
}

export async function getUnreadCount(): Promise<number> {
  const response = await apiClient.get<UnreadCountResponse>('/notifications/unread-count');
  return response.data.count;
}

export async function markAsRead(notificationId: number): Promise<void> {
  await apiClient.put(`/notifications/${notificationId}/read`);
}

export async function markAllAsRead(): Promise<void> {
  await apiClient.put('/notifications/read-all');
}
