import apiClient from './client';
import type { Favorite } from '@/types/favorite';

export async function getFavorites(): Promise<Favorite[]> {
  const response = await apiClient.get<Favorite[]>('/favorites');
  return response.data;
}

export async function addFavorite(eventId: number): Promise<Favorite> {
  const response = await apiClient.post<Favorite>('/favorites', { eventId });
  return response.data;
}

export async function removeFavorite(eventId: number): Promise<void> {
  await apiClient.delete(`/favorites/${eventId}`);
}

export async function checkFavorite(eventId: number): Promise<boolean> {
  const response = await apiClient.get<{ favorited: boolean }>(
    `/favorites/check/${eventId}`,
  );
  return response.data.favorited;
}
