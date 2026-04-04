import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { MobileNav } from './MobileNav';
import { useAuthStore } from '@/stores/auth-store';
import { useUiStore } from '@/stores/ui-store';
import { useCartStore } from '@/stores/cart-store';

const mockLogout = vi.fn();

vi.mock('@/hooks/use-auth', () => ({
  useLogout: () => mockLogout,
}));

describe('MobileNav', () => {
  beforeEach(() => {
    mockLogout.mockReset();
    // Reset stores to defaults
    useAuthStore.setState({
      user: null,
      accessToken: null,
      isAuthenticated: false,
    });
    useUiStore.setState({
      mobileNavOpen: true,
    });
    useCartStore.setState({
      itemCount: 0,
    });
  });

  it('renders app name in the sheet header', () => {
    renderWithProviders(<MobileNav />);

    expect(screen.getByText('MockHub')).toBeDefined();
  });

  it('renders Events link', () => {
    renderWithProviders(<MobileNav />);

    expect(screen.getByText('Events')).toBeDefined();
  });

  it('shows login and signup buttons when not authenticated', () => {
    renderWithProviders(<MobileNav />);

    expect(screen.getByText('Log in')).toBeDefined();
    expect(screen.getByText('Sign up')).toBeDefined();
  });

  it('shows user info and navigation when authenticated', () => {
    useAuthStore.setState({
      user: {
        id: 1,
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        phone: null,
        avatarUrl: null,
        roles: ['ROLE_USER'],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });

    renderWithProviders(<MobileNav />);

    expect(screen.getByText('John Doe')).toBeDefined();
    expect(screen.getByText('john@example.com')).toBeDefined();
    expect(screen.getByText('Profile')).toBeDefined();
    expect(screen.getByText('Cart')).toBeDefined();
    expect(screen.getByText('My Orders')).toBeDefined();
    expect(screen.getByText('Favorites')).toBeDefined();
    expect(screen.getByText('Notifications')).toBeDefined();
    expect(screen.getByText('Sell Tickets')).toBeDefined();
    expect(screen.getByText('My Listings')).toBeDefined();
    expect(screen.getByText('Earnings')).toBeDefined();
    expect(screen.getByText('Log out')).toBeDefined();
  });

  it('does not show login/signup when authenticated', () => {
    useAuthStore.setState({
      user: {
        id: 1,
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        phone: null,
        avatarUrl: null,
        roles: ['ROLE_USER'],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });

    renderWithProviders(<MobileNav />);

    expect(screen.queryByText('Sign up')).toBeNull();
  });

  it('shows admin link for admin users', () => {
    useAuthStore.setState({
      user: {
        id: 1,
        firstName: 'Admin',
        lastName: 'User',
        email: 'admin@example.com',
        phone: null,
        avatarUrl: null,
        roles: ['ROLE_USER', 'ROLE_ADMIN'],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });

    renderWithProviders(<MobileNav />);

    expect(screen.getByText('Admin Dashboard')).toBeDefined();
  });

  it('does not show admin link for non-admin users', () => {
    useAuthStore.setState({
      user: {
        id: 1,
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        phone: null,
        avatarUrl: null,
        roles: ['ROLE_USER'],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });

    renderWithProviders(<MobileNav />);

    expect(screen.queryByText('Admin Dashboard')).toBeNull();
  });

  it('shows cart item count badge when items in cart', () => {
    useAuthStore.setState({
      user: {
        id: 1,
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        phone: null,
        avatarUrl: null,
        roles: ['ROLE_USER'],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });
    useCartStore.setState({ itemCount: 3 });

    renderWithProviders(<MobileNav />);

    expect(screen.getByText('3')).toBeDefined();
  });

  it('does not show cart count badge when cart is empty', () => {
    useAuthStore.setState({
      user: {
        id: 1,
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        phone: null,
        avatarUrl: null,
        roles: ['ROLE_USER'],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });
    useCartStore.setState({ itemCount: 0 });

    renderWithProviders(<MobileNav />);

    // Cart text should exist but no count badge
    expect(screen.getByText('Cart')).toBeDefined();
    expect(screen.queryByText('0')).toBeNull();
  });
});
