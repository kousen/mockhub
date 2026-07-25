import { beforeEach, describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { ApprovalsPage, parseSnapshot } from './ApprovalsPage';
import type { AgentPurchaseApproval } from '@/types/agentApproval';

vi.mock('@/hooks/use-agent-approvals', () => ({
  useMyApprovals: vi.fn(() => ({ data: undefined, isLoading: false })),
  usePendingApprovalCount: vi.fn(() => 0),
  useApproveApproval: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useDenyApproval: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
}));

import { useMyApprovals, useApproveApproval, useDenyApproval } from '@/hooks/use-agent-approvals';

const FUTURE = new Date(Date.now() + 30 * 60 * 1000).toISOString();
const PAST = new Date(Date.now() - 5 * 60 * 1000).toISOString();

function approval(overrides: Partial<AgentPurchaseApproval>): AgentPurchaseApproval {
  return {
    approvalId: 'approval-001',
    userEmail: 'user@example.com',
    agentId: 'claude',
    mandateId: 'mandate-001',
    status: 'PROPOSED',
    proposedOrderSnapshot: JSON.stringify({
      items: [
        { eventName: 'The Spinners', sectionName: 'Floor', row: 'C', seat: '7', unitPrice: 40.51 },
      ],
    }),
    agentRationale: 'Best value adjacent pair on the floor.',
    subtotal: 75.35,
    serviceFee: 7.54,
    total: 82.89,
    commercePolicySnapshot: null,
    proposedAt: '2026-07-25T10:00:00Z',
    approvedAt: null,
    deniedAt: null,
    expiresAt: FUTURE,
    completedAt: null,
    failedAt: null,
    finalOrderNumber: null,
    denialReason: null,
    failureReason: null,
    createdAt: '2026-07-25T10:00:00Z',
    ...overrides,
  };
}

function setApprovals(data: AgentPurchaseApproval[] | undefined, isLoading = false) {
  vi.mocked(useMyApprovals).mockReturnValue({
    data,
    isLoading,
  } as unknown as ReturnType<typeof useMyApprovals>);
}

describe('ApprovalsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useApproveApproval).mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
    } as unknown as ReturnType<typeof useApproveApproval>);
    vi.mocked(useDenyApproval).mockReturnValue({
      mutate: vi.fn(),
      isPending: false,
    } as unknown as ReturnType<typeof useDenyApproval>);
  });

  it('renders a pending proposal with agent, totals, rationale, and snapshot', () => {
    setApprovals([approval({})]);

    renderWithProviders(<ApprovalsPage />);

    expect(screen.getByText('Purchase Approvals')).toBeDefined();
    expect(screen.getByText('claude')).toBeDefined();
    expect(screen.getByText('$82.89')).toBeDefined();
    expect(screen.getByText(/Best value adjacent pair/)).toBeDefined();
    expect(screen.getByText('The Spinners')).toBeDefined();
    expect(screen.getByText(/Expires in/)).toBeDefined();
  });

  it('approve button triggers the approve mutation', async () => {
    const mutate = vi.fn();
    vi.mocked(useApproveApproval).mockReturnValue({
      mutate,
      isPending: false,
    } as unknown as ReturnType<typeof useApproveApproval>);
    setApprovals([approval({})]);

    const user = userEvent.setup();
    renderWithProviders(<ApprovalsPage />);
    await user.click(screen.getByRole('button', { name: /Approve/ }));

    expect(mutate).toHaveBeenCalledWith('approval-001', expect.anything());
  });

  it('deny flow reveals reason input and passes reason to the mutation', async () => {
    const mutate = vi.fn();
    vi.mocked(useDenyApproval).mockReturnValue({
      mutate,
      isPending: false,
    } as unknown as ReturnType<typeof useDenyApproval>);
    setApprovals([approval({})]);

    const user = userEvent.setup();
    renderWithProviders(<ApprovalsPage />);
    await user.click(screen.getByRole('button', { name: /^Deny$/ }));
    await user.type(screen.getByLabelText('Denial reason'), 'Too expensive');
    await user.click(screen.getByRole('button', { name: /Confirm Deny/ }));

    expect(mutate).toHaveBeenCalledWith(
      { approvalId: 'approval-001', reason: 'Too expensive' },
      expect.anything(),
    );
  });

  it('disables actions for an expired proposal', () => {
    setApprovals([approval({ expiresAt: PAST })]);

    renderWithProviders(<ApprovalsPage />);

    expect(screen.getByText('Expired')).toBeDefined();
    expect(screen.getByRole('button', { name: /Approve/ })).toHaveProperty('disabled', true);
  });

  it('shows the empty state when nothing is pending', () => {
    setApprovals([]);

    renderWithProviders(<ApprovalsPage />);

    expect(screen.getByText('Nothing waiting for you')).toBeDefined();
  });

  it('renders history with status badge, order number, and denial reason', () => {
    setApprovals([
      approval({
        approvalId: 'approval-done',
        status: 'COMPLETED',
        completedAt: '2026-07-24T21:15:35Z',
        finalOrderNumber: 'MH-20260724-0001',
      }),
      approval({
        approvalId: 'approval-denied',
        status: 'DENIED',
        deniedAt: '2026-07-23T12:00:00Z',
        denialReason: 'Wrong seats',
      }),
    ]);

    renderWithProviders(<ApprovalsPage />);

    expect(screen.getByText('History')).toBeDefined();
    expect(screen.getByText('COMPLETED')).toBeDefined();
    expect(screen.getByText(/Order MH-20260724-0001/)).toBeDefined();
    expect(screen.getByText(/Denied: Wrong seats/)).toBeDefined();
  });
});

describe('parseSnapshot', () => {
  it('parses an items-array snapshot', () => {
    const items = parseSnapshot(
      JSON.stringify({ items: [{ event: 'Show', section: 'A', price: 10 }] }),
    );
    expect(items).toHaveLength(1);
    expect(items?.[0].event).toBe('Show');
    expect(items?.[0].unitPrice).toBe(10);
  });

  it('parses a bare array snapshot', () => {
    const items = parseSnapshot(JSON.stringify([{ eventName: 'Show 2', seat: '12' }]));
    expect(items?.[0].event).toBe('Show 2');
    expect(items?.[0].seat).toBe('12');
  });

  it('returns null for free text', () => {
    expect(parseSnapshot('two floor seats please')).toBeNull();
  });

  it('returns null for null input', () => {
    expect(parseSnapshot(null)).toBeNull();
  });
});
