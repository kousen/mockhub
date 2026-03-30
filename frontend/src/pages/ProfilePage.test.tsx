import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { ProfilePage } from './ProfilePage';
import { useAuthStore } from '@/stores/auth-store';

const MOCK_USER = {
  id: 2,
  email: 'buyer@mockhub.com',
  firstName: 'Jane',
  lastName: 'Buyer',
  phone: '555-0101',
  avatarUrl: null,
  roles: ['ROLE_USER'],
};

vi.mock('@/hooks/use-spotify', () => ({
  useSpotifyConnection: () => ({ data: undefined }),
  useDisconnectSpotify: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock('@/hooks/use-auth', () => ({
  useCurrentUser: () => ({ data: MOCK_USER }),
  useUpdateProfile: () => ({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
  }),
  useLinkedProviders: () => ({ data: ['google'] }),
  useUnlinkProvider: () => ({
    mutate: vi.fn(),
    isPending: false,
  }),
}));

function renderProfilePage() {
  useAuthStore.setState({
    user: MOCK_USER,
    accessToken: 'mock-token',
    isAuthenticated: true,
  });
  return renderWithProviders(<ProfilePage />, { route: '/my/profile' });
}

describe('ProfilePage', () => {
  it('renders profile heading', () => {
    renderProfilePage();

    expect(screen.getByRole('heading', { name: /profile/i })).toBeDefined();
  });

  it('displays user email as disabled input', () => {
    renderProfilePage();

    const emailInput = screen.getByLabelText('Email') as HTMLInputElement;
    expect(emailInput.value).toBe('buyer@mockhub.com');
    expect(emailInput.disabled).toBe(true);
  });

  it('displays editable first name and last name', () => {
    renderProfilePage();

    const firstNameInput = screen.getByLabelText('First Name') as HTMLInputElement;
    const lastNameInput = screen.getByLabelText('Last Name') as HTMLInputElement;
    expect(firstNameInput.value).toBe('Jane');
    expect(lastNameInput.value).toBe('Buyer');
  });

  it('displays phone number', () => {
    renderProfilePage();

    const phoneInput = screen.getByLabelText('Phone Number') as HTMLInputElement;
    expect(phoneInput.value).toBe('555-0101');
  });

  it('shows save button', () => {
    renderProfilePage();

    expect(screen.getByRole('button', { name: /save changes/i })).toBeDefined();
  });

  it('shows connected services with provider status', () => {
    renderProfilePage();

    expect(screen.getByText('Connected Services')).toBeDefined();
    expect(screen.getByText('Google')).toBeDefined();
    expect(screen.getByText('GitHub')).toBeDefined();
    expect(screen.getByText('Spotify')).toBeDefined();
    expect(screen.getByRole('button', { name: /disconnect/i })).toBeDefined();
  });
});
