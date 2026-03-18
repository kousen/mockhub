import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
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
 * Auth store using Zustand with sessionStorage persistence.
 *
 * The JWT access token is persisted to sessionStorage so it survives
 * page refreshes within the same browser tab. sessionStorage is scoped
 * to the tab and cleared when the tab closes, which is a reasonable
 * compromise between security and usability for a teaching app.
 *
 * A production app would use HttpOnly refresh cookies and in-memory
 * access tokens, with the backend's /auth/refresh endpoint restoring
 * the session on page load.
 */
export const useAuthStore = create<AuthState & AuthActions>()(
  persist(
    (set) => ({
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
    }),
    {
      name: 'mockhub-auth',
      storage: createJSONStorage(() => sessionStorage),
    },
  ),
);
