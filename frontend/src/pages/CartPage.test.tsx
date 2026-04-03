import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { CartPage } from './CartPage';

const mockRemoveMutate = vi.fn();
const mockClearMutate = vi.fn();

vi.mock('@/hooks/use-cart', () => ({
  useCart: vi.fn(),
  useRemoveFromCart: () => ({
    mutate: mockRemoveMutate,
    isPending: false,
  }),
  useClearCart: () => ({
    mutate: mockClearMutate,
    isPending: false,
  }),
}));

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = { isAuthenticated: true, user: null, accessToken: 'mock-token' };
    return selector(state);
  }),
}));

vi.mock('@/stores/cart-store', () => ({
  useCartStore: vi.fn((selector) => {
    const state = { itemCount: 0, openDrawer: vi.fn(), setItemCount: vi.fn() };
    return selector(state);
  }),
}));

import { useCart } from '@/hooks/use-cart';
const mockedUseCart = vi.mocked(useCart);

describe('CartPage', () => {
  it('shows loading skeletons when loading', () => {
    mockedUseCart.mockReturnValue({
      data: undefined,
      isLoading: true,
    } as unknown as ReturnType<typeof useCart>);

    const { container } = renderWithProviders(<CartPage />);
    const skeletons = container.querySelectorAll('[data-slot="skeleton"]');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('shows empty cart state when cart has no items', () => {
    mockedUseCart.mockReturnValue({
      data: { id: 1, userId: 1, items: [], subtotal: 0, itemCount: 0, expiresAt: null },
      isLoading: false,
    } as unknown as ReturnType<typeof useCart>);

    renderWithProviders(<CartPage />);
    expect(screen.getByText('Your cart is empty')).toBeDefined();
    expect(
      screen.getByText('Find tickets for your favorite events and add them here.'),
    ).toBeDefined();
    expect(screen.getByRole('link', { name: 'Browse Events' })).toBeDefined();
  });

  it('shows empty cart state when cart is undefined', () => {
    mockedUseCart.mockReturnValue({
      data: undefined,
      isLoading: false,
    } as unknown as ReturnType<typeof useCart>);

    renderWithProviders(<CartPage />);
    expect(screen.getByText('Your cart is empty')).toBeDefined();
  });

  it('renders cart items and header when cart has items', () => {
    mockedUseCart.mockReturnValue({
      data: {
        id: 1,
        userId: 1,
        items: [
          {
            id: 101,
            listingId: 201,
            eventName: 'Rock Festival 2026',
            eventSlug: 'rock-festival-2026',
            sectionName: 'Floor A',
            rowLabel: 'R1',
            seatNumber: '15',
            ticketType: 'STANDARD',
            priceAtAdd: 150.0,
            currentPrice: 155.0,
            addedAt: '2026-03-15T10:00:00Z',
          },
        ],
        subtotal: 155.0,
        itemCount: 1,
        expiresAt: null,
      },
      isLoading: false,
    } as unknown as ReturnType<typeof useCart>);

    renderWithProviders(<CartPage />);
    expect(screen.getByText('Shopping Cart')).toBeDefined();
    expect(screen.getByText('Clear Cart')).toBeDefined();
    expect(screen.getByText('Rock Festival 2026')).toBeDefined();
  });
});
