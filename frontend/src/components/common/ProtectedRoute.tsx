import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuthStore } from '@/stores/auth-store';
import { ROUTES } from '@/lib/constants';

/**
 * Route guard that redirects unauthenticated users to the login page.
 * Preserves the intended destination so the user can be redirected
 * back after logging in.
 */
export function ProtectedRoute() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} state={{ from: location }} replace />;
  }

  return <Outlet />;
}
