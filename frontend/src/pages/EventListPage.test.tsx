import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { EventListPage } from './EventListPage';

vi.mock('@/hooks/use-events', () => ({
  useEvents: () => ({
    data: {
      content: [
        {
          id: 1,
          name: 'Rock Festival 2026',
          slug: 'rock-festival-2026',
          artistName: 'The Rockers',
          venueName: 'Madison Square Garden',
          city: 'New York',
          eventDate: '2026-06-15T20:00:00Z',
          minPrice: 75.0,
          availableTickets: 500,
          primaryImageUrl: null,
          categoryName: 'Concert',
          isFeatured: true,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    },
    isLoading: false,
  }),
  useCategories: () => ({
    data: [
      { id: 1, name: 'Concert', slug: 'concert', icon: 'music', sortOrder: 1 },
    ],
    isLoading: false,
  }),
}));

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = { isAuthenticated: false, user: null, token: null };
    return selector(state);
  }),
}));

vi.mock('@/stores/cart-store', () => ({
  useCartStore: vi.fn((selector) => {
    const state = { itemCount: 0, openDrawer: vi.fn() };
    return selector(state);
  }),
}));

describe('EventListPage', () => {
  it('renders "Browse Events" heading', () => {
    renderWithProviders(<EventListPage />, { route: '/events' });
    expect(screen.getByText('Browse Events')).toBeDefined();
  });

  it('renders event count', async () => {
    renderWithProviders(<EventListPage />, { route: '/events' });
    await waitFor(() => {
      expect(screen.getByText('1 event found')).toBeDefined();
    });
  });

  it('renders sort dropdown', () => {
    renderWithProviders(<EventListPage />, { route: '/events' });
    expect(screen.getByText('Sort by')).toBeDefined();
  });
});
