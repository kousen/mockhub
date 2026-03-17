import { createBrowserRouter } from 'react-router';
import { MainLayout } from '@/components/layout/MainLayout';
import { ProtectedRoute } from '@/components/common/ProtectedRoute';
import { AdminRoute } from '@/components/common/AdminRoute';
import { HomePage } from '@/pages/HomePage';
import { LoginPage } from '@/pages/LoginPage';
import { RegisterPage } from '@/pages/RegisterPage';
import { NotFoundPage } from '@/pages/NotFoundPage';

/**
 * Placeholder component for routes that will be built in future waves.
 */
function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="flex min-h-[calc(100vh-10rem)] items-center justify-center">
      <div className="text-center">
        <h1 className="text-3xl font-bold">{title}</h1>
        <p className="mt-2 text-muted-foreground">This page is coming soon.</p>
      </div>
    </div>
  );
}

export const router = createBrowserRouter([
  {
    path: '/',
    Component: MainLayout,
    children: [
      { index: true, Component: HomePage },
      { path: 'login', Component: LoginPage },
      { path: 'register', Component: RegisterPage },
      {
        path: 'events',
        Component: () => <PlaceholderPage title="Events" />,
      },
      {
        path: 'events/:slug',
        Component: () => <PlaceholderPage title="Event Details" />,
      },

      // Protected routes (require authentication)
      {
        Component: ProtectedRoute,
        children: [
          {
            path: 'cart',
            Component: () => <PlaceholderPage title="Shopping Cart" />,
          },
          {
            path: 'checkout',
            Component: () => <PlaceholderPage title="Checkout" />,
          },
          {
            path: 'orders',
            Component: () => <PlaceholderPage title="My Orders" />,
          },
          {
            path: 'favorites',
            Component: () => <PlaceholderPage title="My Favorites" />,
          },
        ],
      },

      // Admin routes (require ROLE_ADMIN)
      {
        Component: AdminRoute,
        children: [
          {
            path: 'admin',
            Component: () => <PlaceholderPage title="Admin Dashboard" />,
          },
          {
            path: 'admin/*',
            Component: () => <PlaceholderPage title="Admin" />,
          },
        ],
      },

      // Catch-all
      { path: '*', Component: NotFoundPage },
    ],
  },
]);
