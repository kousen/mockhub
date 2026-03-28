import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import * as authApi from '@/api/auth';
import { useAuthStore } from '@/stores/auth-store';
import type { LoginRequest, RegisterRequest, UpdateProfileRequest } from '@/types/auth';

export function useLinkedProviders() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return useQuery({
    queryKey: ['auth', 'providers'],
    queryFn: () => authApi.getLinkedProviders(),
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,
  });
}
import { ROUTES } from '@/lib/constants';

/**
 * Hook for logging in. On success, stores the token and user in the auth store
 * and navigates to the home page.
 */
export function useLogin() {
  const login = useAuthStore((state) => state.login);
  const navigate = useNavigate();

  return useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: (response) => {
      login(response.accessToken, response.user);
      navigate(ROUTES.HOME);
    },
  });
}

/**
 * Hook for registering a new account. On success, stores the token and user
 * in the auth store and navigates to the home page.
 */
export function useRegister() {
  const login = useAuthStore((state) => state.login);
  const navigate = useNavigate();

  return useMutation({
    mutationFn: (data: RegisterRequest) => authApi.register(data),
    onSuccess: (response) => {
      login(response.accessToken, response.user);
      navigate(ROUTES.HOME);
    },
  });
}

/**
 * Hook for logging out. Clears the auth store and invalidates all queries.
 */
export function useLogout() {
  const logout = useAuthStore((state) => state.logout);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return () => {
    logout();
    queryClient.clear();
    navigate(ROUTES.LOGIN);
  };
}

/**
 * Hook for fetching the current user profile.
 * Only enabled when the user is authenticated (has a token).
 */
export function useCurrentUser() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const setUser = useAuthStore((state) => state.setUser);

  const query = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: () => authApi.getMe(),
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  useEffect(() => {
    if (query.data) {
      setUser(query.data);
    }
  }, [query.data, setUser]);

  return query;
}

/**
 * Hook for updating the current user's profile.
 * On success, updates the auth store with the new user data.
 */
export function useUpdateProfile() {
  const setUser = useAuthStore((state) => state.setUser);
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateProfileRequest) => authApi.updateMe(data),
    onSuccess: (updatedUser) => {
      setUser(updatedUser);
      queryClient.invalidateQueries({ queryKey: ['auth', 'me'] });
    },
  });
}
