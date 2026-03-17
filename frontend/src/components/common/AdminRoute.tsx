import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuthStore } from '@/stores/auth-store';
import { ROUTES } from '@/lib/constants';

/**
 * Route guard that requires both authentication and ROLE_ADMIN.
 * Unauthenticated users go to /login; authenticated non-admins go to /.
 */
export function AdminRoute() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const user = useAuthStore((state) => state.user);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} state={{ from: location }} replace />;
  }

  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false;
  if (!isAdmin) {
    return <Navigate to={ROUTES.HOME} replace />;
  }

  return <Outlet />;
}
