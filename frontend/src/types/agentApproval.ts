export type AgentApprovalStatus =
  | 'PROPOSED'
  | 'APPROVED'
  | 'DENIED'
  | 'EXPIRED'
  | 'COMPLETED'
  | 'FAILED';

export interface AgentPurchaseApproval {
  approvalId: string;
  userEmail: string;
  agentId: string;
  mandateId: string;
  status: AgentApprovalStatus;
  proposedOrderSnapshot: string | null;
  agentRationale: string | null;
  subtotal: number;
  serviceFee: number;
  total: number;
  commercePolicySnapshot: string | null;
  proposedAt: string;
  approvedAt: string | null;
  deniedAt: string | null;
  expiresAt: string | null;
  completedAt: string | null;
  failedAt: string | null;
  finalOrderNumber: string | null;
  denialReason: string | null;
  failureReason: string | null;
  createdAt: string;
}
