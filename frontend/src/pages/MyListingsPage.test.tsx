import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { MyListingsPage } from './MyListingsPage';
import type { SellerListing } from '@/types/seller';

vi.mock('@/hooks/use-seller', () => ({
  useMyListings: vi.fn(() => ({
    data: undefined,
    isLoading: false,
  })),
  useUpdatePrice: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
  useDeactivateListing: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
}));

import { useMyListings, useUpdatePrice, useDeactivateListing } from '@/hooks/use-seller';

const mockListings: SellerListing[] = [
  {
    id: 1,
    ticketId: 101,
    eventSlug: 'taylor-swift-eras-tour',
    eventName: 'Taylor Swift - Eras Tour',
    eventDate: '2026-06-15T20:00:00',
    venueName: 'SoFi Stadium',
    sectionName: 'Floor',
    rowLabel: 'A',
    seatNumber: '5',
    ticketType: 'STANDARD',
    listedPrice: 250.0,
    computedPrice: 250.0,
    faceValue: 150.0,
    status: 'ACTIVE',
    listedAt: '2026-03-01T10:00:00',
    createdAt: '2026-03-01T10:00:00',
  },
  {
    id: 2,
    ticketId: 102,
    eventSlug: 'kendrick-lamar',
    eventName: 'Kendrick Lamar',
    eventDate: '2026-07-20T19:30:00',
    venueName: 'Madison Square Garden',
    sectionName: 'Section 101',
    rowLabel: '12',
    seatNumber: '8',
    ticketType: 'STANDARD',
    listedPrice: 120.0,
    computedPrice: 120.0,
    faceValue: 85.0,
    status: 'SOLD',
    listedAt: '2026-02-15T14:00:00',
    createdAt: '2026-02-15T14:00:00',
  },
];

function setListings(data: SellerListing[] | undefined, isLoading = false) {
  vi.mocked(useMyListings).mockReturnValue({
    data,
    isLoading,
  } as ReturnType<typeof useMyListings>);
}

describe('MyListingsPage', () => {
  it('renders listing cards when listings exist', () => {
    setListings(mockListings);

    renderWithProviders(<MyListingsPage />);

    expect(screen.getByText('My Listings')).toBeDefined();
    // Event names appear in both mobile card and desktop table layouts
    expect(screen.getAllByText('Taylor Swift - Eras Tour').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Kendrick Lamar').length).toBeGreaterThan(0);
  });

  it('shows empty state when no listings', () => {
    setListings([]);

    renderWithProviders(<MyListingsPage />);

    expect(screen.getByText('No listings yet')).toBeDefined();
    expect(
      screen.getByText('When you list tickets for sale, they will appear here.'),
    ).toBeDefined();
  });

  it('renders tab filters', () => {
    setListings(mockListings);

    renderWithProviders(<MyListingsPage />);

    expect(screen.getByRole('tab', { name: 'All' })).toBeDefined();
    expect(screen.getByRole('tab', { name: 'Active' })).toBeDefined();
    expect(screen.getByRole('tab', { name: 'Sold' })).toBeDefined();
    expect(screen.getByRole('tab', { name: 'Cancelled' })).toBeDefined();
  });

  it('switches tab filter when clicked', async () => {
    setListings(mockListings);

    const user = userEvent.setup();
    renderWithProviders(<MyListingsPage />);

    const activeTab = screen.getByRole('tab', { name: 'Active' });
    await user.click(activeTab);

    // The hook should be called with 'ACTIVE' status filter
    expect(vi.mocked(useMyListings)).toHaveBeenCalledWith('ACTIVE');
  });

  it('shows loading skeleton when loading', () => {
    setListings(undefined, true);

    const { container } = renderWithProviders(<MyListingsPage />);

    // Skeleton components should be rendered (they don't show "My Listings" heading)
    const skeletons = container.querySelectorAll('[class*="animate-pulse"]');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('shows "Sell Tickets" link', () => {
    setListings(mockListings);

    renderWithProviders(<MyListingsPage />);

    const sellLinks = screen.getAllByText('Sell Tickets');
    expect(sellLinks.length).toBeGreaterThan(0);
  });

  it('shows status badges for listings', () => {
    setListings(mockListings);

    renderWithProviders(<MyListingsPage />);

    // Status badges appear in both mobile card and desktop table layouts
    expect(screen.getAllByText('ACTIVE').length).toBeGreaterThan(0);
    expect(screen.getAllByText('SOLD').length).toBeGreaterThan(0);
  });

  it('shows empty state with filtered message when filter has no results', async () => {
    // First render with listings for ALL
    setListings(mockListings);
    const user = userEvent.setup();
    renderWithProviders(<MyListingsPage />);

    // Switch to Cancelled tab — mock returns empty for this filter
    setListings([]);
    const cancelledTab = screen.getByRole('tab', { name: 'Cancelled' });
    await user.click(cancelledTab);

    expect(screen.getByText('No listings yet')).toBeDefined();
    expect(screen.getByText('You have no cancelled listings.')).toBeDefined();
  });
});
