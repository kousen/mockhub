import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as approvalsApi from '@/api/agentApprovals';
import { useAuthStore } from '@/stores/auth-store';

const QUERY_KEY = ['agent-approvals'];
const PENDING_POLL_MS = 30_000;

export function useMyApprovals() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  return useQuery({
    queryKey: QUERY_KEY,
    queryFn: () => approvalsApi.getMyApprovals(),
    enabled: isAuthenticated,
    // A user opening the site mid-agent-session should see waiting proposals
    // without a manual refresh.
    refetchInterval: PENDING_POLL_MS,
  });
}

/**
 * A proposal is actionable only while PROPOSED and unexpired — the cleanup job
 * persists the EXPIRED status on a 15-minute cadence, so the client must not
 * trust status alone.
 */
export function isActionablePending(approval: {
  status: string;
  expiresAt?: string | null;
}): boolean {
  return (
    approval.status === 'PROPOSED' &&
    (approval.expiresAt == null || Date.parse(approval.expiresAt) > Date.now())
  );
}

export function usePendingApprovalCount(): number {
  const { data } = useMyApprovals();
  return data?.filter(isActionablePending).length ?? 0;
}

export function useApproveApproval() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (approvalId: string) => approvalsApi.approveApproval(approvalId),
    // Returning the promise keeps isPending true until the refetch lands,
    // so the stale PROPOSED card cannot be clicked twice.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEY }),
  });
}

export function useDenyApproval() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ approvalId, reason }: { approvalId: string; reason?: string }) =>
      approvalsApi.denyApproval(approvalId, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEY }),
  });
}
