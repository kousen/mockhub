import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import {
  useMyApprovals,
  usePendingApprovalCount,
  useApproveApproval,
  useDenyApproval,
} from './use-agent-approvals';

vi.mock('@/api/agentApprovals', () => ({
  getMyApprovals: vi.fn().mockResolvedValue([]),
  approveApproval: vi.fn().mockResolvedValue({ approvalId: 'a-1', status: 'APPROVED' }),
  denyApproval: vi.fn().mockResolvedValue({ approvalId: 'a-1', status: 'DENIED' }),
}));

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector: (state: { isAuthenticated: boolean }) => boolean) =>
    selector({ isAuthenticated: true }),
  ),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe('use-agent-approvals hooks', () => {
  beforeEach(() => vi.clearAllMocks());

  it('useMyApprovals fetches approvals', async () => {
    const { result } = renderHook(() => useMyApprovals(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });

  it('usePendingApprovalCount counts only PROPOSED approvals', async () => {
    const approvalsApi = await import('@/api/agentApprovals');
    vi.mocked(approvalsApi.getMyApprovals).mockResolvedValue([
      { approvalId: 'a-1', status: 'PROPOSED' },
      { approvalId: 'a-2', status: 'PROPOSED' },
      { approvalId: 'a-3', status: 'DENIED' },
    ] as Awaited<ReturnType<typeof approvalsApi.getMyApprovals>>);

    const { result } = renderHook(() => usePendingApprovalCount(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current).toBe(2));
  });

  it('useApproveApproval calls approveApproval', async () => {
    const approvalsApi = await import('@/api/agentApprovals');
    const { result } = renderHook(() => useApproveApproval(), { wrapper: createWrapper() });

    result.current.mutate('approval-123');

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(approvalsApi.approveApproval).toHaveBeenCalledWith('approval-123');
  });

  it('useDenyApproval passes reason through', async () => {
    const approvalsApi = await import('@/api/agentApprovals');
    const { result } = renderHook(() => useDenyApproval(), { wrapper: createWrapper() });

    result.current.mutate({ approvalId: 'approval-123', reason: 'nope' });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(approvalsApi.denyApproval).toHaveBeenCalledWith('approval-123', 'nope');
  });
});
