import apiClient from './client';
import type { PublicOrderView } from '@/types/public-ticket';

export async function getPublicOrderView(token: string): Promise<PublicOrderView> {
  const response = await apiClient.get<PublicOrderView>('/tickets/view', {
    params: { token },
  });
  return response.data;
}
