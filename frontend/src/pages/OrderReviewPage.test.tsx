import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { OrderReviewPage } from './OrderReviewPage';
import type { Cart } from '@/types/cart';
import { hasReviewPassed } from '@/lib/checkout-review-gate';

const navigateMock = vi.fn();

vi.mock('react-router', async (importActual) => {
  const actual = await importActual<typeof import('react-router')>();
  return {
    ...actual,
    useNavigate: () => navigateMock,
  };
});

vi.mock('@/hooks/use-cart', () => ({
  useCart: vi.fn(() => ({ data: undefined, isLoading: false })),
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
      currentPrice: 275.0,
      addedAt: '2026-03-20T10:00:00',
    },
  ],
  subtotal: 275.0,
  itemCount: 1,
  expiresAt: null,
};

function setCartState(data: Cart | undefined, isLoading = false) {
  vi.mocked(useCart).mockReturnValue({
    data,
    isLoading,
  } as unknown as ReturnType<typeof useCart>);
}

describe('OrderReviewPage', () => {
  beforeEach(() => {
    sessionStorage.clear();
    navigateMock.mockReset();
  });

  it('renders empty state when cart is empty', () => {
    setCartState({ ...mockCart, items: [], itemCount: 0, subtotal: 0 });
    renderWithProviders(<OrderReviewPage />);
    expect(screen.getByText('Nothing to review')).toBeDefined();
  });

  it('renders empty state when cart is undefined', () => {
    setCartState(undefined);
    renderWithProviders(<OrderReviewPage />);
    expect(screen.getByText('Nothing to review')).toBeDefined();
  });

  it('renders itemized order with subtotal, fee, and total', () => {
    setCartState(mockCart);
    renderWithProviders(<OrderReviewPage />);

    expect(screen.getByRole('heading', { level: 1, name: 'Order Review' })).toBeDefined();
    expect(screen.getByText('Taylor Swift - Eras Tour')).toBeDefined();
    // Service fee (10% of 275) and total are computed in CartSummary
    expect(screen.getByText('Service Fee (10%)')).toBeDefined();
    expect(screen.getByText('$27.50')).toBeDefined(); // fee
    expect(screen.getByText('$302.50')).toBeDefined(); // total
  });

  it('preserves the price-change signal for repriced items', () => {
    setCartState(mockCart); // priceAtAdd=250, currentPrice=275 -> +$25
    renderWithProviders(<OrderReviewPage />);
    expect(screen.getByText(/\+\$25\.00 since added/)).toBeDefined();
  });

  it('Back to Cart link points at /cart', () => {
    setCartState(mockCart);
    renderWithProviders(<OrderReviewPage />);
    const back = screen.getByRole('link', { name: 'Back to Cart' });
    expect(back.getAttribute('href')).toBe('/cart');
  });

  it('Continue to Payment marks the gate and navigates to /checkout', async () => {
    setCartState(mockCart);
    renderWithProviders(<OrderReviewPage />);

    await userEvent.click(screen.getByRole('button', { name: 'Continue to Payment' }));

    expect(hasReviewPassed(mockCart)).toBe(true);
    expect(navigateMock).toHaveBeenCalledWith('/checkout');
  });
});
