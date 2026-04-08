import { createBrowserRouter } from 'react-router';
import { MainLayout } from '@/components/layout/MainLayout';
import { ProtectedRoute } from '@/components/common/ProtectedRoute';
import { AdminRoute } from '@/components/common/AdminRoute';
import { HomePage } from '@/pages/HomePage';
import { LoginPage } from '@/pages/LoginPage';
import { RegisterPage } from '@/pages/RegisterPage';
import { AuthCallbackPage } from '@/pages/AuthCallbackPage';
import { NotFoundPage } from '@/pages/NotFoundPage';
import { TicketVerifyPage } from '@/pages/TicketVerifyPage';
import { PublicTicketViewPage } from '@/pages/PublicTicketViewPage';
import { EventListPage } from '@/pages/EventListPage';
import { EventDetailPage } from '@/pages/EventDetailPage';
import { CartPage } from '@/pages/CartPage';
import { CheckoutPage } from '@/pages/CheckoutPage';
import { OrderHistoryPage } from '@/pages/OrderHistoryPage';
import { OrderConfirmationPage } from '@/pages/OrderConfirmationPage';
import { FavoritesPage } from '@/pages/FavoritesPage';
import { SellPage } from '@/pages/SellPage';
import { MyListingsPage } from '@/pages/MyListingsPage';
import { EarningsPage } from '@/pages/EarningsPage';
import { MandatesPage } from '@/pages/MandatesPage';
import { ProfilePage } from '@/pages/ProfilePage';
import { AdminLayout } from '@/components/admin/AdminLayout';
import { AdminDashboardPage } from '@/pages/admin/AdminDashboardPage';
import { AdminEventsPage } from '@/pages/admin/AdminEventsPage';
import { AdminEventFormPage } from '@/pages/admin/AdminEventFormPage';
import { AdminUsersPage } from '@/pages/admin/AdminUsersPage';

export const router = createBrowserRouter([
  {
    path: '/',
    Component: MainLayout,
    children: [
      { index: true, Component: HomePage },
      { path: 'login', Component: LoginPage },
      { path: 'register', Component: RegisterPage },
      { path: 'events', Component: EventListPage },
      { path: 'events/:slug', Component: EventDetailPage },
      { path: 'auth/callback', Component: AuthCallbackPage },
      { path: 'verify', Component: TicketVerifyPage },
      { path: 'tickets/view', Component: PublicTicketViewPage },

      // Protected routes (require authentication)
      {
        Component: ProtectedRoute,
        children: [
          { path: 'cart', Component: CartPage },
          { path: 'checkout', Component: CheckoutPage },
          { path: 'orders', Component: OrderHistoryPage },
          {
            path: 'orders/:orderNumber/confirmation',
            Component: OrderConfirmationPage,
          },
          { path: 'favorites', Component: FavoritesPage },
          { path: 'my/profile', Component: ProfilePage },
          { path: 'sell', Component: SellPage },
          { path: 'my/listings', Component: MyListingsPage },
          { path: 'my/earnings', Component: EarningsPage },
          { path: 'my/mandates', Component: MandatesPage },
        ],
      },

      // Admin routes (require ROLE_ADMIN)
      {
        Component: AdminRoute,
        children: [
          {
            path: 'admin',
            Component: AdminLayout,
            children: [
              { index: true, Component: AdminDashboardPage },
              { path: 'events', Component: AdminEventsPage },
              { path: 'events/new', Component: AdminEventFormPage },
              { path: 'events/:id/edit', Component: AdminEventFormPage },
              { path: 'users', Component: AdminUsersPage },
            ],
          },
        ],
      },

      // Catch-all
      { path: '*', Component: NotFoundPage },
    ],
  },
]);
