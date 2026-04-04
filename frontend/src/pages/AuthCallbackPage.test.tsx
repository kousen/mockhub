import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { AuthCallbackPage } from './AuthCallbackPage';

vi.mock('@/api/auth', () => ({
  exchangeOAuth2Code: vi.fn().mockResolvedValue({
    accessToken: 'test-token',
    user: {
      id: 1,
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      roles: ['ROLE_USER'],
    },
  }),
}));

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: (selector: (state: { login: () => void }) => unknown) =>
    selector({ login: vi.fn() }),
}));

describe('AuthCallbackPage', () => {
  it('shows error when error param is present', () => {
    renderWithProviders(<AuthCallbackPage />, { route: '/auth/callback?error=access_denied' });

    expect(screen.getByText('Sign-in Error')).toBeDefined();
    expect(screen.getByText('Authentication failed. Please try again.')).toBeDefined();
  });

  it('shows specific error for oauth_no_email', () => {
    renderWithProviders(<AuthCallbackPage />, { route: '/auth/callback?error=oauth_no_email' });

    expect(
      screen.getByText(
        'Could not retrieve your email from the provider. Please try a different sign-in method.',
      ),
    ).toBeDefined();
  });

  it('shows error when no code param is present', () => {
    renderWithProviders(<AuthCallbackPage />, { route: '/auth/callback' });

    expect(screen.getByText('Sign-in Error')).toBeDefined();
    expect(screen.getByText('No authentication code received.')).toBeDefined();
  });

  it('shows loading state when code is present', () => {
    renderWithProviders(<AuthCallbackPage />, { route: '/auth/callback?code=test-code' });

    expect(screen.getByText('Completing sign-in...')).toBeDefined();
  });

  it('renders "Back to Login" button on error', () => {
    renderWithProviders(<AuthCallbackPage />, { route: '/auth/callback?error=test' });

    expect(screen.getByText('Back to Login')).toBeDefined();
  });
});
