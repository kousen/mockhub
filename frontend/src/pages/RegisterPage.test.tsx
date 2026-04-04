import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { RegisterPage } from './RegisterPage';

const mockMutate = vi.fn();

vi.mock('@/hooks/use-auth', () => ({
  useRegister: () => ({
    mutate: mockMutate,
    isPending: false,
    error: null,
    isError: false,
  }),
}));

vi.mock('@/components/auth/SocialLoginButtons', () => ({
  SocialLoginButtons: () => <div data-testid="social-login-buttons">Social Login</div>,
}));

describe('RegisterPage', () => {
  beforeEach(() => {
    mockMutate.mockReset();
  });

  it('renders the registration form', () => {
    renderWithProviders(<RegisterPage />, { route: '/register' });

    expect(screen.getByText('Create an account')).toBeDefined();
    expect(screen.getByText('Join MockHub to buy and sell concert tickets')).toBeDefined();
  });

  it('renders all form fields', () => {
    renderWithProviders(<RegisterPage />, { route: '/register' });

    expect(screen.getByLabelText('First name')).toBeDefined();
    expect(screen.getByLabelText('Last name')).toBeDefined();
    expect(screen.getByLabelText('Phone Number (optional)')).toBeDefined();
    expect(screen.getByLabelText('Email')).toBeDefined();
    expect(screen.getByLabelText('Password')).toBeDefined();
    expect(screen.getByLabelText('Confirm password')).toBeDefined();
  });

  it('renders submit button', () => {
    renderWithProviders(<RegisterPage />, { route: '/register' });

    expect(screen.getByRole('button', { name: 'Create account' })).toBeDefined();
  });

  it('renders link to login page', () => {
    renderWithProviders(<RegisterPage />, { route: '/register' });

    expect(screen.getByText('Log in')).toBeDefined();
    expect(screen.getByText('Already have an account?')).toBeDefined();
  });

  it('renders social login buttons', () => {
    renderWithProviders(<RegisterPage />, { route: '/register' });

    expect(screen.getByTestId('social-login-buttons')).toBeDefined();
  });

  it('shows validation error when passwords do not match', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await user.type(screen.getByLabelText('First name'), 'John');
    await user.type(screen.getByLabelText('Last name'), 'Doe');
    await user.type(screen.getByLabelText('Email'), 'john@example.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.type(screen.getByLabelText('Confirm password'), 'different123');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByText('Passwords do not match.')).toBeDefined();
    expect(mockMutate).not.toHaveBeenCalled();
  });

  it('calls register mutation with form data when passwords match', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await user.type(screen.getByLabelText('First name'), 'John');
    await user.type(screen.getByLabelText('Last name'), 'Doe');
    await user.type(screen.getByLabelText('Email'), 'john@example.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.type(screen.getByLabelText('Confirm password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(mockMutate).toHaveBeenCalledWith({
      email: 'john@example.com',
      password: 'password123',
      firstName: 'John',
      lastName: 'Doe',
    });
  });

  it('includes phone number when provided', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await user.type(screen.getByLabelText('First name'), 'Jane');
    await user.type(screen.getByLabelText('Last name'), 'Smith');
    await user.type(screen.getByLabelText('Phone Number (optional)'), '5551234567');
    await user.type(screen.getByLabelText('Email'), 'jane@example.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.type(screen.getByLabelText('Confirm password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(mockMutate).toHaveBeenCalledWith({
      email: 'jane@example.com',
      password: 'password123',
      firstName: 'Jane',
      lastName: 'Smith',
      phone: '5551234567',
    });
  });

  it('renders submit button with correct text when not pending', () => {
    renderWithProviders(<RegisterPage />, { route: '/register' });

    expect(screen.getByRole('button', { name: 'Create account' })).toBeDefined();
    expect(screen.queryByText('Creating account...')).toBeNull();
  });

  it('does not show error message when no error exists', () => {
    renderWithProviders(<RegisterPage />, { route: '/register' });

    expect(screen.queryByText('Registration failed. Please try again.')).toBeNull();
    expect(screen.queryByText('Passwords do not match.')).toBeNull();
  });

  it('has correct input types for email and password fields', () => {
    renderWithProviders(<RegisterPage />, { route: '/register' });

    const emailInput = screen.getByLabelText('Email') as HTMLInputElement;
    expect(emailInput.type).toBe('email');

    const passwordInput = screen.getByLabelText('Password') as HTMLInputElement;
    expect(passwordInput.type).toBe('password');

    const confirmInput = screen.getByLabelText('Confirm password') as HTMLInputElement;
    expect(confirmInput.type).toBe('password');

    const phoneInput = screen.getByLabelText('Phone Number (optional)') as HTMLInputElement;
    expect(phoneInput.type).toBe('tel');
  });

  it('clears validation error on resubmit', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />, { route: '/register' });

    // First: trigger password mismatch
    await user.type(screen.getByLabelText('First name'), 'John');
    await user.type(screen.getByLabelText('Last name'), 'Doe');
    await user.type(screen.getByLabelText('Email'), 'john@example.com');
    await user.type(screen.getByLabelText('Password'), 'password123');
    await user.type(screen.getByLabelText('Confirm password'), 'different123');
    await user.click(screen.getByRole('button', { name: 'Create account' }));
    expect(screen.getByText('Passwords do not match.')).toBeDefined();

    // Clear confirm and type matching password
    await user.clear(screen.getByLabelText('Confirm password'));
    await user.type(screen.getByLabelText('Confirm password'), 'password123');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    // Validation error should be cleared, mutation should be called
    expect(screen.queryByText('Passwords do not match.')).toBeNull();
    expect(mockMutate).toHaveBeenCalled();
  });
});
