import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { RecommendationsSection } from './RecommendationsSection';
import type { Recommendation } from '@/types/ai';

const mockRecommendations: Recommendation[] = [
  {
    eventId: 1,
    eventName: 'Taylor Swift - Eras Tour',
    eventSlug: 'taylor-swift-eras-tour',
    venueName: 'SoFi Stadium',
    city: 'Los Angeles',
    eventDate: '2026-06-15T20:00:00',
    minPrice: 150.0,
    relevanceScore: 0.95,
    reason: 'Based on your interest in pop concerts',
  },
  {
    eventId: 2,
    eventName: 'Kendrick Lamar',
    eventSlug: 'kendrick-lamar',
    venueName: 'Madison Square Garden',
    city: 'New York',
    eventDate: '2026-07-20T19:30:00',
    minPrice: 85.0,
    relevanceScore: 0.88,
    reason: 'Popular in your area',
  },
];

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector: (state: Record<string, unknown>) => unknown) => {
    const state = { isAuthenticated: false, user: null, accessToken: null };
    return selector(state);
  }),
}));

vi.mock('@/hooks/use-ai', () => ({
  useRecommendations: vi.fn(() => ({
    data: undefined,
    isLoading: false,
  })),
}));

import { useAuthStore } from '@/stores/auth-store';
import { useRecommendations } from '@/hooks/use-ai';

function setAuthenticated(value: boolean) {
  vi.mocked(useAuthStore).mockImplementation((selector) => {
    const state = {
      isAuthenticated: value,
      user: null,
      accessToken: value ? 'token' : null,
    };
    return selector(state as Parameters<typeof selector>[0]);
  });
}

function setRecommendations(data: Recommendation[] | undefined, isLoading = false) {
  vi.mocked(useRecommendations).mockReturnValue({
    data,
    isLoading,
  } as ReturnType<typeof useRecommendations>);
}

describe('RecommendationsSection', () => {
  it('renders "Recommended for You" heading when authenticated', () => {
    setAuthenticated(true);
    setRecommendations(mockRecommendations);

    renderWithProviders(<RecommendationsSection />);

    expect(screen.getByText('Recommended for You')).toBeDefined();
  });

  it('renders "Trending Events" heading when not authenticated', () => {
    setAuthenticated(false);
    setRecommendations(mockRecommendations);

    renderWithProviders(<RecommendationsSection />);

    expect(screen.getByText('Trending Events')).toBeDefined();
  });

  it('renders recommendation cards when data available', () => {
    setAuthenticated(true);
    setRecommendations(mockRecommendations);

    renderWithProviders(<RecommendationsSection />);

    expect(screen.getByText('Taylor Swift - Eras Tour')).toBeDefined();
    expect(screen.getByText('Kendrick Lamar')).toBeDefined();
    expect(screen.getByText('95%')).toBeDefined();
    expect(screen.getByText('88%')).toBeDefined();
  });

  it('renders nothing when no recommendations and not loading', () => {
    setAuthenticated(true);
    setRecommendations(undefined);

    const { container } = renderWithProviders(<RecommendationsSection />);

    expect(container.innerHTML).toBe('');
  });

  it('renders skeleton when loading', () => {
    setAuthenticated(true);
    setRecommendations(undefined, true);

    renderWithProviders(<RecommendationsSection />);

    expect(screen.getByText('Recommended for You')).toBeDefined();
  });
});
