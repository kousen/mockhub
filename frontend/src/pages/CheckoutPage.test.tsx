import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { CheckoutPage } from './CheckoutPage';
import type { Cart } from '@/types/cart';

vi.mock('@/hooks/use-cart', () => ({
  useCart: vi.fn(() => ({
    data: undefined,
    isLoading: false,
  })),
}));

vi.mock('@/hooks/use-orders', () => ({
  useCheckout: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
}));

vi.mock('@/api/payments', () => ({
  createPaymentIntent: vi.fn(),
  confirmPayment: vi.fn(),
}));

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector: (state: Record<string, unknown>) => unknown) => {
    const state = { isAuthenticated: true, user: null, accessToken: 'token' };
    return selector(state);
  }),
}));

vi.mock('@/stores/cart-store', () => ({
  useCartStore: vi.fn((selector: (state: Record<string, unknown>) => unknown) => {
    const state = { itemCount: 0, setItemCount: vi.fn() };
    return selector(state);
  }),
}));

import { useCart } from '@/hooks/use-cart';

const mockCart: Cart = {
  id: 1,
  userId: 1,
  items: [
    {
      id: 101,
      listingId: 10,
      eventName: 'Taylor Swift - Eras Tour',
      eventSlug: 'taylor-swift-eras-tour',
      sectionName: 'Floor A',
      rowLabel: 'Row 5',
      seatNumber: '12',
      ticketType: 'STANDARD',
      priceAtAdd: 250.0,
      currentPrice: 250.0,
      addedAt: '2026-03-20T10:00:00',
    },
    {
      id: 102,
      listingId: 11,
      eventName: 'Kendrick Lamar',
      eventSlug: 'kendrick-lamar',
      sectionName: 'Section 200',
      rowLabel: 'Row 10',
      seatNumber: '8',
      ticketType: 'STANDARD',
      priceAtAdd: 85.0,
      currentPrice: 85.0,
      addedAt: '2026-03-20T10:05:00',
    },
  ],
  subtotal: 335.0,
  itemCount: 2,
  expiresAt: null,
};

function setCartState(data: Cart | undefined, isLoading = false) {
  vi.mocked(useCart).mockReturnValue({
    data,
    isLoading,
  } as ReturnType<typeof useCart>);
}

describe('CheckoutPage', () => {
  it('renders order summary when cart has items', () => {
    setCartState(mockCart);

    renderWithProviders(<CheckoutPage />);

    expect(screen.getByText('Checkout')).toBeDefined();
    expect(screen.getByText('Payment Method')).toBeDefined();
  });

  it('shows empty cart message when no items', () => {
    setCartState(undefined);

    renderWithProviders(<CheckoutPage />);

    expect(screen.getByText('Nothing to checkout')).toBeDefined();
    expect(screen.getByText('Add tickets to your cart first.')).toBeDefined();
  });

  it('shows empty cart message when cart has zero items', () => {
    setCartState({ ...mockCart, items: [], itemCount: 0 });

    renderWithProviders(<CheckoutPage />);

    expect(screen.getByText('Nothing to checkout')).toBeDefined();
  });

  it('shows loading skeleton while fetching', () => {
    setCartState(undefined, true);

    const { container } = renderWithProviders(<CheckoutPage />);

    // Skeleton elements are rendered (div with pulse animation classes)
    const skeletons = container.querySelectorAll('[class*="animate-pulse"]');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('payment method tabs are visible when cart has items', () => {
    setCartState(mockCart);

    renderWithProviders(<CheckoutPage />);

    // "Mock Payment" appears in both the tab trigger and the form heading
    expect(screen.getAllByText('Mock Payment').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Stripe')).toBeDefined();
  });

  it('renders order review with cart items', () => {
    setCartState(mockCart);

    renderWithProviders(<CheckoutPage />);

    expect(screen.getByText('Taylor Swift - Eras Tour')).toBeDefined();
    expect(screen.getByText('Kendrick Lamar')).toBeDefined();
  });

  it('renders checkout heading', () => {
    setCartState(mockCart);

    renderWithProviders(<CheckoutPage />);

    expect(screen.getByRole('heading', { name: 'Checkout' })).toBeDefined();
  });

  it('shows stripe tab', () => {
    setCartState(mockCart);

    renderWithProviders(<CheckoutPage />);

    expect(screen.getByText('Stripe')).toBeDefined();
  });
});
