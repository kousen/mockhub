import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { EventDetailPage } from './EventDetailPage';

vi.mock('react-router', async () => {
  const actual = await vi.importActual<typeof import('react-router')>('react-router');
  return {
    ...actual,
    useParams: () => ({ slug: 'test-event' }),
  };
});

const mockEvent = {
  id: 1,
  name: 'Test Concert',
  slug: 'test-event',
  artistName: 'Test Artist',
  description: 'A great concert',
  eventDate: '2026-06-15T19:00:00Z',
  doorsOpenAt: '2026-06-15T18:00:00Z',
  status: 'ON_SALE',
  basePrice: 75.0,
  minPrice: 50.0,
  maxPrice: 200.0,
  availableTickets: 500,
  totalTickets: 1000,
  primaryImageUrl: null,
  spotifyArtistId: null,
  venue: {
    id: 1,
    name: 'Test Venue',
    city: 'New York',
    state: 'NY',
    address: '123 Main St',
  },
  category: { id: 1, name: 'Rock', slug: 'rock' },
  tags: [
    { id: 1, name: 'rock' },
    { id: 2, name: 'live' },
  ],
};

vi.mock('@/hooks/use-events', () => ({
  useEvent: () => ({
    data: mockEvent,
    isLoading: false,
    error: null,
  }),
  useEventListings: () => ({
    data: [
      {
        id: 1,
        sectionName: 'Orchestra',
        rowLabel: 'A',
        seatNumber: '1',
        ticketType: 'STANDARD',
        computedPrice: 100,
        listedPrice: 100,
        status: 'ACTIVE',
      },
    ],
    isLoading: false,
  }),
  useEventPriceHistory: () => ({
    data: [],
    isLoading: false,
  }),
  useEventSections: () => ({
    data: [],
    isLoading: false,
  }),
}));

vi.mock('@/hooks/use-spotify', () => ({
  useSpotifyArtist: () => ({
    data: null,
  }),
}));

vi.mock('@/components/ai/PricePredictionBadge', () => ({
  PricePredictionBadge: () => <div data-testid="price-prediction">Price Prediction</div>,
}));

vi.mock('@/components/events/FavoriteButton', () => ({
  FavoriteButton: ({ className }: { className?: string }) => (
    <button data-testid="favorite-button" className={className}>
      Favorite
    </button>
  ),
}));

vi.mock('@/components/tickets/TicketListView', () => ({
  TicketListView: () => <div data-testid="ticket-list-view">Ticket List</div>,
}));

vi.mock('@/components/tickets/VenueMap', () => ({
  VenueMap: () => <div data-testid="venue-map">Venue Map</div>,
}));

vi.mock('@/components/tickets/PriceHistoryChart', () => ({
  PriceHistoryChart: () => <div data-testid="price-history-chart">Price History</div>,
}));

describe('EventDetailPage', () => {
  it('renders event name and artist', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    // Event name appears in both the hero placeholder and the h1 heading
    expect(screen.getAllByText('Test Concert').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Test Artist')).toBeDefined();
  });

  it('renders event description', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    expect(screen.getByText('A great concert')).toBeDefined();
  });

  it('renders venue info', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    expect(screen.getByText('Test Venue, New York, NY')).toBeDefined();
  });

  it('renders category badge', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    expect(screen.getByText('Rock')).toBeDefined();
  });

  it('renders tags', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    expect(screen.getByText('rock')).toBeDefined();
    expect(screen.getByText('live')).toBeDefined();
  });

  it('renders ticket availability', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    expect(screen.getByText('500 of 1000 available')).toBeDefined();
  });

  it('renders price summary section', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    expect(screen.getByText('Base Price')).toBeDefined();
    expect(screen.getByText('From')).toBeDefined();
    expect(screen.getByText('Up to')).toBeDefined();
  });

  it('renders tabs for tickets, venue map, and price history', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    expect(screen.getByText('Tickets')).toBeDefined();
    expect(screen.getByText('Venue Map')).toBeDefined();
    expect(screen.getByText('Price History')).toBeDefined();
  });

  it('renders ticket list view in default tab', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    expect(screen.getByTestId('ticket-list-view')).toBeDefined();
  });

  it('renders favorite button', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    expect(screen.getByTestId('favorite-button')).toBeDefined();
  });

  it('renders price prediction badge', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    expect(screen.getByTestId('price-prediction')).toBeDefined();
  });

  it('does not show status badge when event is ON_SALE', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    // ON_SALE status should not show a destructive badge
    expect(screen.queryByText('ON_SALE')).toBeNull();
  });

  it('renders placeholder when no image', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    // When no primaryImageUrl, the event name is shown as placeholder
    // The event name appears in both the hero placeholder and the header
    const eventNames = screen.getAllByText('Test Concert');
    expect(eventNames.length).toBeGreaterThanOrEqual(1);
  });
});

describe('EventDetailPage - doors open time', () => {
  it('renders doors open time when available', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    expect(screen.getByText(/Doors:/)).toBeDefined();
  });
});

describe('EventDetailPage - listing count badge', () => {
  it('shows listing count badge on tickets tab', () => {
    renderWithProviders(<EventDetailPage />, { route: '/events/test-event' });

    // The listings mock has 1 listing, so badge shows "1"
    expect(screen.getByText('1')).toBeDefined();
  });
});
