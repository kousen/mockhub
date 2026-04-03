import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { FavoritesPage } from './FavoritesPage';

vi.mock('@/hooks/use-favorites', () => ({
  useFavorites: vi.fn(),
  useCheckFavorite: () => ({ data: false, isLoading: false }),
  useAddFavorite: () => ({ mutate: vi.fn(), isPending: false }),
  useRemoveFavorite: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = { isAuthenticated: true, user: null, accessToken: 'mock-token' };
    return selector(state);
  }),
}));

vi.mock('@/stores/cart-store', () => ({
  useCartStore: vi.fn((selector) => {
    const state = { itemCount: 0, openDrawer: vi.fn() };
    return selector(state);
  }),
}));

import { useFavorites } from '@/hooks/use-favorites';
const mockedUseFavorites = vi.mocked(useFavorites);

describe('FavoritesPage', () => {
  it('renders the page heading', () => {
    mockedUseFavorites.mockReturnValue({
      data: [],
      isLoading: false,
    } as unknown as ReturnType<typeof useFavorites>);

    renderWithProviders(<FavoritesPage />);
    expect(screen.getByText('My Favorites')).toBeDefined();
  });

  it('shows loading skeletons when loading', () => {
    mockedUseFavorites.mockReturnValue({
      data: undefined,
      isLoading: true,
    } as unknown as ReturnType<typeof useFavorites>);

    const { container } = renderWithProviders(<FavoritesPage />);
    // Skeleton elements are rendered (8 of them)
    const skeletons = container.querySelectorAll(
      '[class*="animate-pulse"], [data-slot="skeleton"]',
    );
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('shows empty state when no favorites', () => {
    mockedUseFavorites.mockReturnValue({
      data: [],
      isLoading: false,
    } as unknown as ReturnType<typeof useFavorites>);

    renderWithProviders(<FavoritesPage />);
    expect(screen.getByText('No favorites yet')).toBeDefined();
    expect(
      screen.getByText('Browse events and tap the heart icon to save your favorites here.'),
    ).toBeDefined();
    expect(screen.getByRole('link', { name: 'Browse Events' })).toBeDefined();
  });

  it('shows empty state when favorites is undefined', () => {
    mockedUseFavorites.mockReturnValue({
      data: undefined,
      isLoading: false,
    } as unknown as ReturnType<typeof useFavorites>);

    renderWithProviders(<FavoritesPage />);
    expect(screen.getByText('No favorites yet')).toBeDefined();
  });

  it('renders event cards when favorites are present', () => {
    mockedUseFavorites.mockReturnValue({
      data: [
        {
          id: 1,
          eventId: 10,
          event: {
            id: 10,
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
          createdAt: '2026-03-01T00:00:00Z',
        },
      ],
      isLoading: false,
    } as unknown as ReturnType<typeof useFavorites>);

    renderWithProviders(<FavoritesPage />);
    expect(screen.getAllByText('Rock Festival 2026').length).toBeGreaterThan(0);
    expect(screen.queryByText('No favorites yet')).toBeNull();
  });
});
