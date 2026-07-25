import { describe, it, expect, vi } from 'vitest';
import { getMyApprovals, getApproval, approveApproval, denyApproval } from './agentApprovals';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock' }),
    post: vi.fn().mockResolvedValue({ data: 'mock' }),
  },
}));

describe('agentApprovals API', () => {
  it('getMyApprovals calls /agent-approvals', async () => {
    const client = (await import('./client')).default;
    await getMyApprovals();
    expect(client.get).toHaveBeenCalledWith('/agent-approvals');
  });

  it('getApproval calls /agent-approvals/{approvalId}', async () => {
    const client = (await import('./client')).default;
    await getApproval('approval-123');
    expect(client.get).toHaveBeenCalledWith('/agent-approvals/approval-123');
  });

  it('approveApproval posts to /agent-approvals/{approvalId}/approve', async () => {
    const client = (await import('./client')).default;
    await approveApproval('approval-123');
    expect(client.post).toHaveBeenCalledWith('/agent-approvals/approval-123/approve');
  });

  it('denyApproval posts reason body when provided', async () => {
    const client = (await import('./client')).default;
    await denyApproval('approval-123', 'Too expensive');
    expect(client.post).toHaveBeenCalledWith('/agent-approvals/approval-123/deny', {
      reason: 'Too expensive',
    });
  });

  it('denyApproval posts without body when no reason given', async () => {
    const client = (await import('./client')).default;
    await denyApproval('approval-456');
    expect(client.post).toHaveBeenCalledWith('/agent-approvals/approval-456/deny', undefined);
  });
});
