import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { HomePage } from './HomePage';

vi.mock('@/hooks/use-events', () => ({
  useFeaturedEvents: () => ({
    data: [],
    isLoading: false,
  }),
  useCategories: () => ({
    data: [],
    isLoading: false,
  }),
}));

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = { isAuthenticated: false, user: null, accessToken: null };
    return selector(state);
  }),
}));

vi.mock('@/stores/cart-store', () => ({
  useCartStore: vi.fn((selector) => {
    const state = { itemCount: 0, openDrawer: vi.fn() };
    return selector(state);
  }),
}));

// Mock complex child components to keep tests focused on the hero section
vi.mock('@/components/ai/RecommendationsSection', () => ({
  RecommendationsSection: () => <div data-testid="recommendations-section" />,
}));

describe('HomePage', () => {
  it('renders hero headline', () => {
    renderWithProviders(<HomePage />);
    expect(screen.getByText('Find Your Next')).toBeDefined();
    expect(screen.getByText('Live Experience')).toBeDefined();
  });

  it('renders hero description', () => {
    renderWithProviders(<HomePage />);
    expect(screen.getByText(/secondary ticket marketplace/)).toBeDefined();
  });

  it('renders search input in hero', () => {
    renderWithProviders(<HomePage />);
    expect(screen.getByPlaceholderText('Search events, artists, venues...')).toBeDefined();
  });

  it('renders "Browse Events" CTA button in hero', () => {
    renderWithProviders(<HomePage />);
    const browseLinks = screen.getAllByRole('link', { name: /Browse Events/i });
    // The hero CTA should be among the Browse Events links
    expect(browseLinks.length).toBeGreaterThanOrEqual(1);
    const heroCta = browseLinks.find((link) => link.getAttribute('href') === '/events');
    expect(heroCta).toBeDefined();
  });

  it('renders "Built for Learning" section', () => {
    renderWithProviders(<HomePage />);
    expect(screen.getByText('Built for Learning')).toBeDefined();
  });

  it('renders Featured Events section', () => {
    renderWithProviders(<HomePage />);
    expect(screen.getByText('Featured Events')).toBeDefined();
  });
});
