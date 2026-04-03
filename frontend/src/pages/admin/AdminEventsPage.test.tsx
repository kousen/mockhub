import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { AdminEventsPage } from './AdminEventsPage';

const mockEvents = {
  content: [
    {
      id: 1,
      name: 'Summer Jazz Festival',
      slug: 'summer-jazz-festival',
      artistName: 'Miles Davis Tribute',
      status: 'ACTIVE',
      eventDate: '2026-07-20T19:00:00Z',
      totalTicketCount: 500,
      soldTicketCount: 120,
      totalRevenue: 15000.0,
      basePrice: 75.0,
      minPrice: 50.0,
      maxPrice: 150.0,
      totalTickets: 500,
      availableTickets: 380,
      isFeatured: true,
      description: null,
      doorsOpenAt: null,
      venue: {
        id: 1,
        name: 'Central Park',
        slug: 'central-park',
        city: 'New York',
        state: 'NY',
        venueType: 'OUTDOOR',
        capacity: 5000,
        imageUrl: null,
      },
      category: { id: 1, name: 'Jazz', slug: 'jazz', icon: null, sortOrder: 1 },
      tags: [],
      primaryImageUrl: null,
      spotifyArtistId: null,
    },
    {
      id: 2,
      name: 'Rock Night',
      slug: 'rock-night',
      artistName: null,
      status: 'DRAFT',
      eventDate: '2026-08-10T20:00:00Z',
      totalTicketCount: 200,
      soldTicketCount: 0,
      totalRevenue: 0,
      basePrice: 40.0,
      minPrice: null,
      maxPrice: null,
      totalTickets: 200,
      availableTickets: 200,
      isFeatured: false,
      description: null,
      doorsOpenAt: null,
      venue: {
        id: 2,
        name: 'The Arena',
        slug: 'the-arena',
        city: 'Chicago',
        state: 'IL',
        venueType: 'INDOOR',
        capacity: 2000,
        imageUrl: null,
      },
      category: { id: 2, name: 'Rock', slug: 'rock', icon: null, sortOrder: 2 },
      tags: [],
      primaryImageUrl: null,
      spotifyArtistId: null,
    },
  ],
  totalPages: 1,
  number: 0,
  first: true,
  last: true,
};

const mockDeleteMutate = vi.fn();

let eventsReturn = { data: mockEvents, isLoading: false };

vi.mock('@/hooks/use-admin', () => ({
  useAdminEvents: () => eventsReturn,
  useDeleteEvent: () => ({
    mutate: mockDeleteMutate,
    isPending: false,
  }),
}));

describe('AdminEventsPage', () => {
  it('renders loading skeletons when loading', () => {
    eventsReturn = { data: undefined as never, isLoading: true };

    renderWithProviders(<AdminEventsPage />);

    expect(screen.getByText('Events')).toBeDefined();
  });

  it('renders event list with data', () => {
    eventsReturn = { data: mockEvents, isLoading: false };

    renderWithProviders(<AdminEventsPage />);

    expect(screen.getByText('Summer Jazz Festival')).toBeDefined();
    expect(screen.getByText('Miles Davis Tribute')).toBeDefined();
    expect(screen.getByText('ACTIVE')).toBeDefined();
    expect(screen.getByText('120 / 500')).toBeDefined();

    expect(screen.getByText('Rock Night')).toBeDefined();
    expect(screen.getByText('DRAFT')).toBeDefined();
    expect(screen.getByText('0 / 200')).toBeDefined();
  });

  it('renders Create Event button with link', () => {
    eventsReturn = { data: mockEvents, isLoading: false };

    renderWithProviders(<AdminEventsPage />);

    const createLink = screen.getByRole('link', { name: /Create Event/i });
    expect(createLink).toBeDefined();
    expect(createLink.getAttribute('href')).toBe('/admin/events/new');
  });

  it('renders edit links for each event', () => {
    eventsReturn = { data: mockEvents, isLoading: false };

    renderWithProviders(<AdminEventsPage />);

    const editLinks = screen.getAllByRole('link', { name: /Edit/i });
    expect(editLinks).toHaveLength(2);
    expect(editLinks[0].getAttribute('href')).toBe('/admin/events/1/edit');
    expect(editLinks[1].getAttribute('href')).toBe('/admin/events/2/edit');
  });

  it('renders empty state when no events', () => {
    eventsReturn = {
      data: { ...mockEvents, content: [] },
      isLoading: false,
    };

    renderWithProviders(<AdminEventsPage />);

    expect(
      screen.getByText('No events found. Create your first event to get started.'),
    ).toBeDefined();
  });
});
