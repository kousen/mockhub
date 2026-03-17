import { create } from 'zustand';
import type { UserDto } from '@/types/auth';

interface AuthState {
  user: UserDto | null;
  accessToken: string | null;
  isAuthenticated: boolean;
}

interface AuthActions {
  login: (token: string, user: UserDto) => void;
  logout: () => void;
  setUser: (user: UserDto) => void;
  setToken: (token: string) => void;
}

/**
 * Auth store using Zustand.
 *
 * SECURITY NOTE: The JWT access token is stored in memory only (not localStorage).
 * This protects against XSS attacks that could steal tokens from storage.
 * The refresh token is managed via HttpOnly cookies by the backend.
 */
export const useAuthStore = create<AuthState & AuthActions>()((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,

  login: (token: string, user: UserDto) =>
    set({
      accessToken: token,
      user,
      isAuthenticated: true,
    }),

  logout: () =>
    set({
      accessToken: null,
      user: null,
      isAuthenticated: false,
    }),

  setUser: (user: UserDto) => set({ user }),

  setToken: (token: string) => set({ accessToken: token }),
}));
