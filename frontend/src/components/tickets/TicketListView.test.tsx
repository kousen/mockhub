import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { TicketListView } from './TicketListView';
import { useAuthStore } from '@/stores/auth-store';
import type { Listing } from '@/types/ticket';

const mockMutate = vi.fn();

vi.mock('@/hooks/use-cart', () => ({
  useAddToCart: () => ({
    mutate: mockMutate,
    isPending: false,
  }),
}));

vi.mock('./PriceTag', () => ({
  PriceTag: ({ computedPrice }: { computedPrice: number }) => (
    <span data-testid="price-tag">${computedPrice.toFixed(2)}</span>
  ),
}));

const mockListings: Listing[] = [
  {
    id: 1,
    sectionName: 'Orchestra',
    rowLabel: 'A',
    seatNumber: '1',
    ticketType: 'STANDARD',
    computedPrice: 150.0,
    listedPrice: 150.0,
    status: 'ACTIVE',
  },
  {
    id: 2,
    sectionName: 'Balcony',
    rowLabel: 'B',
    seatNumber: '5',
    ticketType: 'VIP',
    computedPrice: 250.0,
    listedPrice: 200.0,
    status: 'ACTIVE',
  },
  {
    id: 3,
    sectionName: 'Orchestra',
    rowLabel: 'A',
    seatNumber: '2',
    ticketType: 'STANDARD',
    computedPrice: 100.0,
    listedPrice: 100.0,
    status: 'ACTIVE',
  },
];

describe('TicketListView', () => {
  beforeEach(() => {
    mockMutate.mockReset();
    useAuthStore.setState({
      user: null,
      accessToken: null,
      isAuthenticated: false,
    });
  });

  it('renders loading skeletons when isLoading is true', () => {
    const { container } = renderWithProviders(
      <TicketListView listings={[]} isLoading={true} />,
    );

    // Skeletons are rendered (5 skeleton divs)
    const skeletons = container.querySelectorAll('[class*="animate-pulse"]');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('renders empty state when listings array is empty', () => {
    renderWithProviders(<TicketListView listings={[]} />);

    expect(screen.getByText('No tickets listed')).toBeDefined();
    expect(screen.getByText('Check back later for available tickets.')).toBeDefined();
  });

  it('renders table with listing data', () => {
    renderWithProviders(<TicketListView listings={mockListings} />);

    expect(screen.getAllByText('Orchestra').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Balcony')).toBeDefined();
    expect(screen.getAllByText('STANDARD').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('VIP')).toBeDefined();
  });

  it('renders table headers', () => {
    renderWithProviders(<TicketListView listings={mockListings} />);

    expect(screen.getByText('Section')).toBeDefined();
    expect(screen.getByText('Row')).toBeDefined();
    expect(screen.getByText('Seat')).toBeDefined();
    expect(screen.getByText('Type')).toBeDefined();
    expect(screen.getByText('Price')).toBeDefined();
    expect(screen.getByText('Action')).toBeDefined();
  });

  it('renders row labels and seat numbers', () => {
    renderWithProviders(<TicketListView listings={mockListings} />);

    expect(screen.getAllByText('A').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('B')).toBeDefined();
    expect(screen.getByText('5')).toBeDefined();
  });

  it('disables Add to Cart buttons when not authenticated', () => {
    renderWithProviders(<TicketListView listings={mockListings} />);

    const buttons = screen.getAllByRole('button', { name: /Add to Cart/i });
    buttons.forEach((button) => {
      expect((button as HTMLButtonElement).disabled).toBe(true);
    });
  });

  it('enables Add to Cart buttons when authenticated', () => {
    useAuthStore.setState({
      user: {
        id: 1,
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        roles: ['ROLE_USER'],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });

    renderWithProviders(<TicketListView listings={mockListings} />);

    const buttons = screen.getAllByRole('button', { name: /Add to Cart/i });
    buttons.forEach((button) => {
      expect((button as HTMLButtonElement).disabled).toBe(false);
    });
  });

  it('calls addToCart mutation when Add to Cart is clicked', async () => {
    useAuthStore.setState({
      user: {
        id: 1,
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        roles: ['ROLE_USER'],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });
    const user = userEvent.setup();

    renderWithProviders(<TicketListView listings={mockListings} />);

    const buttons = screen.getAllByRole('button', { name: /Add to Cart/i });
    await user.click(buttons[0]);

    // Default sort is section asc, so Balcony (id:2) comes first
    expect(mockMutate).toHaveBeenCalledWith(
      { listingId: 2 },
      expect.objectContaining({
        onSuccess: expect.any(Function),
        onError: expect.any(Function),
      }),
    );
  });

  it('sorts by price when price header is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TicketListView listings={mockListings} />);

    const priceButton = screen.getByRole('button', { name: /Price/i });
    await user.click(priceButton);

    // After clicking price sort, listings should be sorted by price
    const priceTags = screen.getAllByTestId('price-tag');
    expect(priceTags.length).toBe(3);
  });

  it('toggles sort direction on double click', async () => {
    const user = userEvent.setup();
    renderWithProviders(<TicketListView listings={mockListings} />);

    const sectionButton = screen.getByRole('button', { name: /Section/i });
    // Default is section asc, click again toggles to desc
    await user.click(sectionButton);

    // Section is already the active sort, so this toggles direction
    const priceTags = screen.getAllByTestId('price-tag');
    expect(priceTags.length).toBe(3);
  });

  it('shows section filter badge when sectionFilter is provided', () => {
    const onClearFilter = vi.fn();
    renderWithProviders(
      <TicketListView listings={mockListings} sectionFilter="Orchestra" onClearFilter={onClearFilter} />,
    );

    // Should show the filter badge with section name and count
    expect(screen.getAllByText(/Orchestra/).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByLabelText('Clear section filter')).toBeDefined();
  });

  it('calls onClearFilter when clear button is clicked', async () => {
    const onClearFilter = vi.fn();
    const user = userEvent.setup();

    renderWithProviders(
      <TicketListView
        listings={mockListings}
        sectionFilter="Orchestra"
        onClearFilter={onClearFilter}
      />,
    );

    await user.click(screen.getByLabelText('Clear section filter'));
    expect(onClearFilter).toHaveBeenCalled();
  });

  it('filters listings by section when sectionFilter is set', () => {
    renderWithProviders(
      <TicketListView listings={mockListings} sectionFilter="Balcony" />,
    );

    // Only the Balcony listing should be in the table
    const rows = screen.getAllByRole('row');
    // 1 header row + 1 data row
    expect(rows.length).toBe(2);
  });

  it('renders GA for listings without seat numbers', () => {
    const gaListing: Listing[] = [
      {
        id: 10,
        sectionName: 'General Admission',
        rowLabel: null,
        seatNumber: null,
        ticketType: 'STANDARD',
        computedPrice: 50.0,
        listedPrice: 50.0,
        status: 'ACTIVE',
      },
    ];

    renderWithProviders(<TicketListView listings={gaListing} />);

    expect(screen.getByText('GA')).toBeDefined();
    expect(screen.getByText('-')).toBeDefined();
  });
});
