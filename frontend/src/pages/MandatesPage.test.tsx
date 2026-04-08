import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { MandatesPage } from './MandatesPage';
import type { Mandate } from '@/types/mandate';

vi.mock('@/hooks/use-mandates', () => ({
  useMyMandates: vi.fn(() => ({
    data: undefined,
    isLoading: false,
  })),
  useCreateMandate: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
  useRevokeMandate: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
}));

import { useMyMandates } from '@/hooks/use-mandates';

const mockMandates: Mandate[] = [
  {
    id: 1,
    mandateId: 'mandate-001',
    agentId: 'claude-desktop',
    userEmail: 'user@example.com',
    scope: 'PURCHASE',
    maxSpendPerTransaction: 200,
    maxSpendTotal: 1000,
    totalSpent: 150,
    remainingBudget: 850,
    allowedCategories: 'jazz, classical',
    allowedEvents: null,
    status: 'ACTIVE',
    expiresAt: '2027-12-31T23:59:59Z',
    createdAt: '2026-03-01T10:00:00Z',
  },
  {
    id: 2,
    mandateId: 'mandate-002',
    agentId: 'my-agent',
    userEmail: 'user@example.com',
    scope: 'BROWSE',
    maxSpendPerTransaction: null,
    maxSpendTotal: null,
    totalSpent: 0,
    remainingBudget: null,
    allowedCategories: null,
    allowedEvents: null,
    status: 'REVOKED',
    expiresAt: null,
    createdAt: '2026-02-15T14:00:00Z',
  },
];

function setMandates(data: Mandate[] | undefined, isLoading = false) {
  vi.mocked(useMyMandates).mockReturnValue({
    data,
    isLoading,
  } as unknown as ReturnType<typeof useMyMandates>);
}

describe('MandatesPage', () => {
  it('renders mandate data when mandates exist', () => {
    setMandates(mockMandates);

    renderWithProviders(<MandatesPage />);

    expect(screen.getByText('Agent Mandates')).toBeDefined();
    // Active mandate agent ID appears in both mobile card and desktop table layouts
    expect(screen.getAllByText('claude-desktop').length).toBeGreaterThan(0);
  });

  it('shows empty state when no mandates', () => {
    setMandates([]);

    renderWithProviders(<MandatesPage />);

    expect(screen.getByText('No mandates')).toBeDefined();
    expect(
      screen.getByText('Create a mandate to authorize an AI agent to act on your behalf.'),
    ).toBeDefined();
  });

  it('renders tab filters', () => {
    setMandates(mockMandates);

    renderWithProviders(<MandatesPage />);

    expect(screen.getByRole('tab', { name: 'Active' })).toBeDefined();
    expect(screen.getByRole('tab', { name: 'Expired' })).toBeDefined();
    expect(screen.getByRole('tab', { name: 'Revoked' })).toBeDefined();
  });

  it('shows Revoked tab content when clicked', async () => {
    setMandates(mockMandates);

    const user = userEvent.setup();
    renderWithProviders(<MandatesPage />);

    const revokedTab = screen.getByRole('tab', { name: 'Revoked' });
    await user.click(revokedTab);

    // Revoked mandate agent ID appears in both mobile card and desktop table layouts
    expect(screen.getAllByText('my-agent').length).toBeGreaterThan(0);
  });

  it('shows loading skeleton when loading', () => {
    setMandates(undefined, true);

    const { container } = renderWithProviders(<MandatesPage />);

    // Skeleton components render with animate-pulse class
    const skeletons = container.querySelectorAll('[class*="animate-pulse"]');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('shows "New Mandate" button', () => {
    setMandates(mockMandates);

    renderWithProviders(<MandatesPage />);

    expect(screen.getByText('New Mandate')).toBeDefined();
  });

  it('shows create form when "New Mandate" clicked', async () => {
    setMandates(mockMandates);

    const user = userEvent.setup();
    renderWithProviders(<MandatesPage />);

    const newMandateButton = screen.getByText('New Mandate');
    await user.click(newMandateButton);

    expect(screen.getByLabelText(/Agent ID/)).toBeDefined();
    expect(screen.getByLabelText(/Scope/)).toBeDefined();
    expect(screen.getByLabelText(/Per-Transaction Limit/)).toBeDefined();
    expect(screen.getByLabelText(/Total Budget/)).toBeDefined();
  });

  it('shows scope badge for active mandate', () => {
    setMandates(mockMandates);

    renderWithProviders(<MandatesPage />);

    // PURCHASE badge appears in both mobile card and desktop table layouts
    expect(screen.getAllByText('PURCHASE').length).toBeGreaterThan(0);
  });

  it('shows spending summary for mandate with budget', () => {
    setMandates(mockMandates);

    renderWithProviders(<MandatesPage />);

    // Spending summary shows in both mobile card and desktop table layouts
    expect(screen.getAllByText(/\$150\.00 spent/).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/\$1000\.00 total/).length).toBeGreaterThan(0);
  });

  it('shows "No mandates" with filtered message for expired tab', async () => {
    setMandates(mockMandates);

    const user = userEvent.setup();
    renderWithProviders(<MandatesPage />);

    const expiredTab = screen.getByRole('tab', { name: 'Expired' });
    await user.click(expiredTab);

    expect(screen.getByText('No mandates')).toBeDefined();
    expect(screen.getByText('You have no expired mandates.')).toBeDefined();
  });
});
