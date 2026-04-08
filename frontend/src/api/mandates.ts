import apiClient from './client';
import type { Mandate, CreateMandateRequest } from '@/types/mandate';

export async function getMyMandates(): Promise<Mandate[]> {
  const response = await apiClient.get<Mandate[]>('/my/mandates');
  return response.data;
}

export async function createMandate(request: CreateMandateRequest): Promise<Mandate> {
  const response = await apiClient.post<Mandate>('/my/mandates', request);
  return response.data;
}

export async function revokeMandate(mandateId: string): Promise<void> {
  await apiClient.delete(`/my/mandates/${mandateId}`);
}
