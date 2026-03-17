import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { Header } from './Header';

// Default to unauthenticated state
const mockAuthState = {
  isAuthenticated: false,
  user: null,
  token: null,
  setAuth: vi.fn(),
  clearAuth: vi.fn(),
};

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector) => selector(mockAuthState)),
}));

vi.mock('@/stores/ui-store', () => ({
  useUiStore: vi.fn((selector) => {
    const state = { toggleMobileNav: vi.fn(), isMobileNavOpen: false };
    return selector(state);
  }),
}));

vi.mock('@/stores/cart-store', () => ({
  useCartStore: vi.fn((selector) => {
    const state = { itemCount: 0, openDrawer: vi.fn() };
    return selector(state);
  }),
}));

vi.mock('@/hooks/use-auth', () => ({
  useLogout: () => vi.fn(),
}));

describe('Header', () => {
  it('renders app name', () => {
    renderWithProviders(<Header />);
    expect(screen.getByText('MockHub')).toBeDefined();
  });

  it('renders Events navigation link', () => {
    renderWithProviders(<Header />);
    expect(screen.getByText('Events')).toBeDefined();
  });

  it('renders Log in and Sign up buttons when unauthenticated', () => {
    renderWithProviders(<Header />);
    expect(screen.getByText('Log in')).toBeDefined();
    expect(screen.getByText('Sign up')).toBeDefined();
  });

  it('renders mobile menu toggle button', () => {
    renderWithProviders(<Header />);
    expect(screen.getByText('Toggle menu')).toBeDefined();
  });
});
