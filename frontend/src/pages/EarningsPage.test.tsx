import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { EarningsPage } from './EarningsPage';
import type { EarningsSummary } from '@/types/seller';

vi.mock('@/hooks/use-seller', () => ({
  useEarnings: vi.fn(() => ({
    data: undefined,
    isLoading: false,
  })),
}));

import { useEarnings } from '@/hooks/use-seller';

const mockEarnings: EarningsSummary = {
  totalEarnings: 500.0,
  totalListings: 10,
  activeListings: 3,
  soldListings: 5,
  recentSales: [
    {
      orderId: 1,
      eventName: 'Taylor Swift - Eras Tour',
      sectionName: 'Floor',
      seatInfo: 'Row A, Seat 5',
      pricePaid: 250.0,
      soldAt: '2026-03-20T14:00:00',
    },
    {
      orderId: 2,
      eventName: 'Kendrick Lamar',
      sectionName: 'Section 101',
      seatInfo: 'Row 12, Seat 8',
      pricePaid: 120.0,
      soldAt: '2026-03-18T10:00:00',
    },
  ],
};

const mockZeroEarnings: EarningsSummary = {
  totalEarnings: 0,
  totalListings: 0,
  activeListings: 0,
  soldListings: 0,
  recentSales: [],
};

function setEarnings(data: EarningsSummary | undefined, isLoading = false) {
  vi.mocked(useEarnings).mockReturnValue({
    data,
    isLoading,
  } as ReturnType<typeof useEarnings>);
}

describe('EarningsPage', () => {
  it('renders earnings summary with stats', () => {
    setEarnings(mockEarnings);

    renderWithProviders(<EarningsPage />);

    expect(screen.getByText('Earnings')).toBeDefined();
    expect(screen.getByText('Total Earnings')).toBeDefined();
    expect(screen.getByText('$500.00')).toBeDefined();
    expect(screen.getByText('Active Listings')).toBeDefined();
    expect(screen.getByText('3')).toBeDefined();
    expect(screen.getByText('Sold Listings')).toBeDefined();
    expect(screen.getByText('5')).toBeDefined();
    expect(screen.getByText('10 total listings')).toBeDefined();
  });

  it('renders recent sales', () => {
    setEarnings(mockEarnings);

    renderWithProviders(<EarningsPage />);

    expect(screen.getByText('Recent Sales')).toBeDefined();
    // Event names appear in both mobile card and desktop table layouts
    expect(screen.getAllByText('Taylor Swift - Eras Tour').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Kendrick Lamar').length).toBeGreaterThan(0);
  });

  it('shows empty state when no earnings data', () => {
    setEarnings(undefined);

    renderWithProviders(<EarningsPage />);

    expect(screen.getByText('No earnings data')).toBeDefined();
    expect(screen.getByText('Start selling tickets to see your earnings here.')).toBeDefined();
    expect(screen.getByText('Sell Tickets')).toBeDefined();
  });

  it('shows "No sales yet" when earnings exist but no recent sales', () => {
    setEarnings(mockZeroEarnings);

    renderWithProviders(<EarningsPage />);

    expect(screen.getByText('No sales yet')).toBeDefined();
    expect(screen.getByText('When your tickets sell, the sales will appear here.')).toBeDefined();
  });

  it('shows loading skeleton when loading', () => {
    setEarnings(undefined, true);

    const { container } = renderWithProviders(<EarningsPage />);

    // Skeleton components render with animate-pulse class
    const skeletons = container.querySelectorAll('[class*="animate-pulse"]');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('renders zero earnings values correctly', () => {
    setEarnings(mockZeroEarnings);

    renderWithProviders(<EarningsPage />);

    expect(screen.getByText('$0.00')).toBeDefined();
    expect(screen.getByText('0 total listings')).toBeDefined();
  });

  it('renders stat descriptions', () => {
    setEarnings(mockEarnings);

    renderWithProviders(<EarningsPage />);

    expect(screen.getByText('Currently for sale')).toBeDefined();
    expect(screen.getByText('Successfully sold')).toBeDefined();
  });
});
