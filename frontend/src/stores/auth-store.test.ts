import { useAuthStore } from '@/stores/auth-store';
import type { UserDto } from '@/types/auth';

const mockUser: UserDto = {
  id: 1,
  email: 'test@example.com',
  firstName: 'John',
  lastName: 'Doe',
  phone: null,
  avatarUrl: null,
  roles: ['ROLE_BUYER'],
};

describe('useAuthStore', () => {
  beforeEach(() => {
    useAuthStore.setState({
      user: null,
      accessToken: null,
      isAuthenticated: false,
    });
    sessionStorage.clear();
  });

  it('login sets token, user, and isAuthenticated', () => {
    useAuthStore.getState().login('test-token', mockUser);

    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('test-token');
    expect(state.user).toEqual(mockUser);
    expect(state.isAuthenticated).toBe(true);
  });

  it('logout clears all state', () => {
    useAuthStore.getState().login('test-token', mockUser);
    useAuthStore.getState().logout();

    const state = useAuthStore.getState();
    expect(state.accessToken).toBeNull();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
  });

  it('setToken updates token without changing user', () => {
    useAuthStore.getState().login('original-token', mockUser);
    useAuthStore.getState().setToken('new-token');

    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('new-token');
    expect(state.user).toEqual(mockUser);
  });

  it('setUser updates user without changing token', () => {
    useAuthStore.getState().login('original-token', mockUser);

    const updatedUser: UserDto = {
      ...mockUser,
      firstName: 'Jane',
      email: 'jane@example.com',
    };
    useAuthStore.getState().setUser(updatedUser);

    const state = useAuthStore.getState();
    expect(state.user).toEqual(updatedUser);
    expect(state.accessToken).toBe('original-token');
  });

  it('persists state to sessionStorage', () => {
    useAuthStore.getState().login('persisted-token', mockUser);

    const stored = sessionStorage.getItem('mockhub-auth');
    expect(stored).not.toBeNull();

    const parsed = JSON.parse(stored!);
    expect(parsed.state.accessToken).toBe('persisted-token');
    expect(parsed.state.user).toEqual(mockUser);
    expect(parsed.state.isAuthenticated).toBe(true);
  });

  it('hydrates state from sessionStorage', () => {
    const seeded = JSON.stringify({
      state: {
        accessToken: 'hydrated-token',
        user: mockUser,
        isAuthenticated: true,
      },
      version: 0,
    });
    sessionStorage.setItem('mockhub-auth', seeded);

    useAuthStore.persist.rehydrate();

    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('hydrated-token');
    expect(state.user).toEqual(mockUser);
    expect(state.isAuthenticated).toBe(true);
  });
});
