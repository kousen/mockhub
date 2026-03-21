import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { FavoriteButton } from './FavoriteButton';

const mockAddFavorite = vi.fn();
const mockRemoveFavorite = vi.fn();

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = { isAuthenticated: false, user: null, accessToken: null };
    return selector(state);
  }),
}));

vi.mock('@/hooks/use-favorites', () => ({
  useCheckFavorite: vi.fn(() => ({ data: false })),
  useAddFavorite: () => ({ mutate: mockAddFavorite, isPending: false }),
  useRemoveFavorite: () => ({ mutate: mockRemoveFavorite, isPending: false }),
}));

// Import after mocks are declared so we can override per-test
import { useAuthStore } from '@/stores/auth-store';
import { useCheckFavorite } from '@/hooks/use-favorites';

function setAuthenticated(value: boolean) {
  vi.mocked(useAuthStore).mockImplementation((selector) => {
    const state = { isAuthenticated: value, user: null, accessToken: 'token' };
    return selector(state as Parameters<typeof selector>[0]);
  });
}

describe('FavoriteButton', () => {
  it('renders a heart button', () => {
    renderWithProviders(<FavoriteButton eventId={1} />);
    const button = screen.getByRole('button');
    expect(button).toBeDefined();
    expect(button.querySelector('svg')).toBeDefined();
  });

  it('is disabled when user is not authenticated', () => {
    setAuthenticated(false);
    renderWithProviders(<FavoriteButton eventId={1} />);
    expect(screen.getByRole('button').hasAttribute('disabled')).toBe(true);
  });

  it('shows tooltip text when user is not authenticated', async () => {
    setAuthenticated(false);
    renderWithProviders(<FavoriteButton eventId={1} />);

    // Hover over the span wrapper to trigger the tooltip
    const trigger = screen.getByRole('button').parentElement!;
    await userEvent.setup().hover(trigger);

    const tooltip = await screen.findByRole('tooltip');
    expect(tooltip.textContent).toContain('Log in to save favorites');
  });

  it('is enabled when user is authenticated', () => {
    setAuthenticated(true);
    renderWithProviders(<FavoriteButton eventId={1} />);
    expect(screen.getByRole('button').hasAttribute('disabled')).toBe(false);
  });

  it('does not wrap with tooltip when authenticated', () => {
    setAuthenticated(true);
    renderWithProviders(<FavoriteButton eventId={1} />);
    // The button should not be inside a tooltip span wrapper
    const button = screen.getByRole('button');
    expect(button.parentElement?.tagName).not.toBe('SPAN');
  });

  it('calls addFavorite when clicked and not yet favorited', async () => {
    setAuthenticated(true);
    vi.mocked(useCheckFavorite).mockReturnValue({ data: false } as ReturnType<typeof useCheckFavorite>);

    renderWithProviders(<FavoriteButton eventId={42} />);
    await userEvent.setup().click(screen.getByRole('button'));

    expect(mockAddFavorite).toHaveBeenCalledWith(42);
  });

  it('calls removeFavorite when clicked and already favorited', async () => {
    setAuthenticated(true);
    vi.mocked(useCheckFavorite).mockReturnValue({ data: true } as ReturnType<typeof useCheckFavorite>);

    renderWithProviders(<FavoriteButton eventId={42} />);
    await userEvent.setup().click(screen.getByRole('button'));

    expect(mockRemoveFavorite).toHaveBeenCalledWith(42);
  });

  it('has accessible label for unfavorited state', () => {
    setAuthenticated(true);
    vi.mocked(useCheckFavorite).mockReturnValue({ data: false } as ReturnType<typeof useCheckFavorite>);

    renderWithProviders(<FavoriteButton eventId={1} />);
    expect(screen.getByLabelText('Add to favorites')).toBeDefined();
  });

  it('has accessible label for favorited state', () => {
    setAuthenticated(true);
    vi.mocked(useCheckFavorite).mockReturnValue({ data: true } as ReturnType<typeof useCheckFavorite>);

    renderWithProviders(<FavoriteButton eventId={1} />);
    expect(screen.getByLabelText('Remove from favorites')).toBeDefined();
  });

  it('is disabled when no eventId is provided', () => {
    setAuthenticated(true);
    renderWithProviders(<FavoriteButton />);
    expect(screen.getByRole('button').hasAttribute('disabled')).toBe(true);
  });
});
