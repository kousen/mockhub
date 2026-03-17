import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { LoginPage } from './LoginPage';

vi.mock('@/hooks/use-auth', () => ({
  useLogin: () => ({
    mutate: vi.fn(),
    isPending: false,
    error: null,
    isError: false,
  }),
}));

describe('LoginPage', () => {
  it('renders login form with email and password fields', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });

    expect(screen.getByLabelText('Email')).toBeDefined();
    expect(screen.getByLabelText('Password')).toBeDefined();
  });

  it('renders "Welcome back" heading', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });

    expect(screen.getByText('Welcome back')).toBeDefined();
  });

  it('renders "Log in" submit button', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });

    expect(screen.getByRole('button', { name: 'Log in' })).toBeDefined();
  });

  it('renders link to sign up page', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });

    expect(screen.getByText('Sign up')).toBeDefined();
  });

  it('renders description text', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });

    expect(screen.getByText('Log in to your MockHub account')).toBeDefined();
  });

  it('email field has correct input type', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });

    const emailInput = screen.getByLabelText('Email') as HTMLInputElement;
    expect(emailInput.type).toBe('email');
  });

  it('password field has correct input type', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });

    const passwordInput = screen.getByLabelText('Password') as HTMLInputElement;
    expect(passwordInput.type).toBe('password');
  });
});
