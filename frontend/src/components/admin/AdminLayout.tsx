import { useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router';
import { LayoutDashboard, CalendarDays, Users, Menu, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { ROUTES } from '@/lib/constants';

interface NavItem {
  label: string;
  path: string;
  icon: React.ElementType;
}

const navItems: NavItem[] = [
  { label: 'Dashboard', path: ROUTES.ADMIN_DASHBOARD, icon: LayoutDashboard },
  { label: 'Events', path: ROUTES.ADMIN_EVENTS, icon: CalendarDays },
  { label: 'Users', path: ROUTES.ADMIN_USERS, icon: Users },
];

/**
 * Admin layout with sidebar navigation and content area.
 * The sidebar collapses into a toggleable overlay on mobile.
 */
export function AdminLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();

  const isActive = (path: string) => {
    if (path === ROUTES.ADMIN_DASHBOARD) {
      return location.pathname === '/admin';
    }
    return location.pathname.startsWith(path);
  };

  const sidebar = (
    <nav className="space-y-1 p-4">
      <h2 className="mb-4 px-3 text-lg font-semibold">Admin</h2>
      {navItems.map((item) => {
        const Icon = item.icon;
        return (
          <Link
            key={item.path}
            to={item.path}
            onClick={() => setSidebarOpen(false)}
            className={cn(
              'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
              isActive(item.path)
                ? 'bg-primary text-primary-foreground'
                : 'text-muted-foreground hover:bg-muted hover:text-foreground',
            )}
          >
            <Icon className="h-4 w-4" />
            {item.label}
          </Link>
        );
      })}
    </nav>
  );

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      {/* Mobile sidebar toggle */}
      <div className="mb-4 md:hidden">
        <Button variant="outline" size="sm" onClick={() => setSidebarOpen(!sidebarOpen)}>
          {sidebarOpen ? <X className="mr-2 h-4 w-4" /> : <Menu className="mr-2 h-4 w-4" />}
          Menu
        </Button>
      </div>

      <div className="flex gap-6">
        {/* Mobile sidebar overlay */}
        {sidebarOpen && (
          <>
            <button
              type="button"
              className="fixed inset-0 z-40 bg-background/80 backdrop-blur-sm md:hidden"
              onClick={() => setSidebarOpen(false)}
              aria-label="Close sidebar"
            />
            <div className="fixed inset-y-0 left-0 z-50 w-64 border-r bg-background pt-20 md:hidden">
              {sidebar}
            </div>
          </>
        )}

        {/* Desktop sidebar */}
        <aside className="hidden w-56 shrink-0 md:block">
          <div className="sticky top-20 rounded-lg border">{sidebar}</div>
        </aside>

        {/* Main content */}
        <main className="min-w-0 flex-1">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
