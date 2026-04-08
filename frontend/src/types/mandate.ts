export interface Mandate {
  id: number;
  mandateId: string;
  agentId: string;
  userEmail: string;
  scope: 'BROWSE' | 'PURCHASE';
  maxSpendPerTransaction: number | null;
  maxSpendTotal: number | null;
  totalSpent: number;
  remainingBudget: number | null;
  allowedCategories: string | null;
  allowedEvents: string | null;
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED';
  expiresAt: string | null;
  createdAt: string;
}

export interface CreateMandateRequest {
  agentId: string;
  scope: 'BROWSE' | 'PURCHASE';
  maxSpendPerTransaction?: number;
  maxSpendTotal?: number;
  allowedCategories?: string;
  allowedEvents?: string;
  expiresAt?: string;
}
