import { Link } from 'react-router';
import { LogOut, Ticket, User } from 'lucide-react';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { useAuthStore } from '@/stores/auth-store';
import { useUiStore } from '@/stores/ui-store';
import { useLogout } from '@/hooks/use-auth';
import { APP_NAME, ROUTES } from '@/lib/constants';

export function MobileNav() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const user = useAuthStore((state) => state.user);
  const mobileNavOpen = useUiStore((state) => state.mobileNavOpen);
  const closeMobileNav = useUiStore((state) => state.closeMobileNav);
  const logout = useLogout();

  const handleLogout = () => {
    closeMobileNav();
    logout();
  };

  return (
    <Sheet open={mobileNavOpen} onOpenChange={closeMobileNav}>
      <SheetContent side="left" className="w-72">
        <SheetHeader>
          <SheetTitle className="flex items-center gap-2">
            <Ticket className="h-5 w-5 text-primary" />
            {APP_NAME}
          </SheetTitle>
        </SheetHeader>
        <nav className="mt-6 flex flex-col gap-2">
          <Link
            to={ROUTES.EVENTS}
            onClick={closeMobileNav}
            className="rounded-md px-3 py-2 text-sm font-medium text-foreground hover:bg-accent"
          >
            Events
          </Link>

          <Separator className="my-2" />

          {isAuthenticated && user ? (
            <>
              <div className="px-3 py-2">
                <p className="text-sm font-medium">
                  {user.firstName} {user.lastName}
                </p>
                <p className="text-xs text-muted-foreground">{user.email}</p>
              </div>
              <Link
                to={ROUTES.ORDERS}
                onClick={closeMobileNav}
                className="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-foreground hover:bg-accent"
              >
                <User className="h-4 w-4" />
                My Orders
              </Link>
              <Link
                to={ROUTES.FAVORITES}
                onClick={closeMobileNav}
                className="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-foreground hover:bg-accent"
              >
                <Ticket className="h-4 w-4" />
                Favorites
              </Link>
              <Separator className="my-2" />
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-foreground hover:bg-accent"
              >
                <LogOut className="h-4 w-4" />
                Log out
              </button>
            </>
          ) : (
            <div className="flex flex-col gap-2 px-3">
              <Button asChild>
                <Link to={ROUTES.LOGIN} onClick={closeMobileNav}>
                  Log in
                </Link>
              </Button>
              <Button variant="outline" asChild>
                <Link to={ROUTES.REGISTER} onClick={closeMobileNav}>
                  Sign up
                </Link>
              </Button>
            </div>
          )}
        </nav>
      </SheetContent>
    </Sheet>
  );
}
