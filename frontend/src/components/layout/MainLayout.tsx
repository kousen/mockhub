import { Outlet } from 'react-router';
import { Header } from './Header';
import { Footer } from './Footer';
import { MobileNav } from './MobileNav';
import { CartDrawer } from '@/components/cart/CartDrawer';
import { ChatWidget } from '@/components/ai/ChatWidget';
import { ErrorBoundary } from '@/components/common/ErrorBoundary';
import { Toaster } from '@/components/ui/sonner';

/**
 * Main application layout with header, footer, cart drawer, and a content area.
 * Uses flexbox to ensure the footer is pushed to the bottom of the viewport.
 * Wraps page content in an ErrorBoundary to catch rendering errors gracefully.
 */
export function MainLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <MobileNav />
      <CartDrawer />
      <main className="flex-1">
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </main>
      <Footer />
      <ChatWidget />
      <Toaster />
    </div>
  );
}
