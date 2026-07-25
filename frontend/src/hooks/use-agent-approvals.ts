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

export function usePendingApprovalCount(): number {
  const { data } = useMyApprovals();
  return data?.filter((a) => a.status === 'PROPOSED').length ?? 0;
}

export function useApproveApproval() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (approvalId: string) => approvalsApi.approveApproval(approvalId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}

export function useDenyApproval() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ approvalId, reason }: { approvalId: string; reason?: string }) =>
      approvalsApi.denyApproval(approvalId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}
