import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { CartDrawer } from './CartDrawer';

// Mock the stores and hooks since CartDrawer depends on them
vi.mock('@/stores/cart-store', () => ({
  useCartStore: vi.fn((selector) => {
    const state = {
      isDrawerOpen: true,
      closeDrawer: vi.fn(),
      itemCount: 0,
      openDrawer: vi.fn(),
    };
    return selector(state);
  }),
}));

vi.mock('@/hooks/use-cart', () => ({
  useCart: () => ({
    data: null,
    isLoading: false,
  }),
  useRemoveFromCart: () => ({
    mutate: vi.fn(),
    isPending: false,
  }),
}));

describe('CartDrawer', () => {
  it('renders "Your Cart" title when open', () => {
    renderWithProviders(<CartDrawer />);
    expect(screen.getByText('Your Cart')).toBeDefined();
  });

  it('shows empty cart message when no items', () => {
    renderWithProviders(<CartDrawer />);
    expect(screen.getByText('Your cart is empty')).toBeDefined();
  });

  it('shows "Browse Events" link in empty cart', () => {
    renderWithProviders(<CartDrawer />);
    expect(screen.getByText('Browse Events')).toBeDefined();
  });
});
