import apiClient from './client';
import type { AgentPurchaseApproval } from '@/types/agentApproval';

export async function getMyApprovals(): Promise<AgentPurchaseApproval[]> {
  const response = await apiClient.get<AgentPurchaseApproval[]>('/agent-approvals');
  return response.data;
}

export async function getApproval(approvalId: string): Promise<AgentPurchaseApproval> {
  const response = await apiClient.get<AgentPurchaseApproval>(`/agent-approvals/${approvalId}`);
  return response.data;
}

export async function approveApproval(approvalId: string): Promise<AgentPurchaseApproval> {
  const response = await apiClient.post<AgentPurchaseApproval>(
    `/agent-approvals/${approvalId}/approve`,
  );
  return response.data;
}

export async function denyApproval(
  approvalId: string,
  reason?: string,
): Promise<AgentPurchaseApproval> {
  const response = await apiClient.post<AgentPurchaseApproval>(
    `/agent-approvals/${approvalId}/deny`,
    reason ? { reason } : undefined,
  );
  return response.data;
}
