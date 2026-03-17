import { Link } from 'react-router';
import { Menu, LogOut, User, Ticket, ShoppingCart, Heart, Settings } from 'lucide-react';
import { NotificationBell } from '@/components/notifications/NotificationBell';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { useAuthStore } from '@/stores/auth-store';
import { useUiStore } from '@/stores/ui-store';
import { useCartStore } from '@/stores/cart-store';
import { useLogout } from '@/hooks/use-auth';
import { APP_NAME, ROUTES } from '@/lib/constants';

function getInitials(firstName: string, lastName: string): string {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
}

export function Header() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const user = useAuthStore((state) => state.user);
  const toggleMobileNav = useUiStore((state) => state.toggleMobileNav);
  const itemCount = useCartStore((state) => state.itemCount);
  const openDrawer = useCartStore((state) => state.openDrawer);
  const logout = useLogout();

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        {/* Logo */}
        <Link to={ROUTES.HOME} className="flex items-center gap-2">
          <Ticket className="h-6 w-6 text-primary" />
          <span className="text-xl font-bold tracking-tight">{APP_NAME}</span>
        </Link>

        {/* Desktop Navigation */}
        <nav className="hidden items-center gap-6 md:flex">
          <Link
            to={ROUTES.EVENTS}
            className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            Events
          </Link>
        </nav>

        {/* Desktop Auth Section */}
        <div className="hidden items-center gap-3 md:flex">
          {isAuthenticated && (
            <>
              <NotificationBell />
              <Button
                variant="ghost"
                size="icon"
                className="relative"
                onClick={openDrawer}
                aria-label="Open cart"
              >
                <ShoppingCart className="h-5 w-5" />
                {itemCount > 0 && (
                  <span className="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground">
                    {itemCount > 99 ? '99+' : itemCount}
                  </span>
                )}
              </Button>
            </>
          )}
          {isAuthenticated && user ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="relative h-9 w-9 rounded-full">
                  <Avatar className="h-9 w-9">
                    <AvatarFallback className="bg-primary text-primary-foreground text-xs">
                      {getInitials(user.firstName, user.lastName)}
                    </AvatarFallback>
                  </Avatar>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <div className="flex items-center gap-2 p-2">
                  <div className="flex flex-col space-y-1">
                    <p className="text-sm font-medium">
                      {user.firstName} {user.lastName}
                    </p>
                    <p className="text-xs text-muted-foreground">{user.email}</p>
                  </div>
                </div>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <Link to={ROUTES.ORDERS}>
                    <User className="mr-2 h-4 w-4" />
                    My Orders
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link to={ROUTES.FAVORITES}>
                    <Heart className="mr-2 h-4 w-4" />
                    Favorites
                  </Link>
                </DropdownMenuItem>
                {user.roles?.includes('ROLE_ADMIN') && (
                  <>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem asChild>
                      <Link to={ROUTES.ADMIN}>
                        <Settings className="mr-2 h-4 w-4" />
                        Admin Dashboard
                      </Link>
                    </DropdownMenuItem>
                  </>
                )}
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={logout}>
                  <LogOut className="mr-2 h-4 w-4" />
                  Log out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <>
              <Button variant="ghost" asChild>
                <Link to={ROUTES.LOGIN}>Log in</Link>
              </Button>
              <Button asChild>
                <Link to={ROUTES.REGISTER}>Sign up</Link>
              </Button>
            </>
          )}
        </div>

        {/* Mobile Menu Button */}
        <Button variant="ghost" size="icon" className="md:hidden" onClick={toggleMobileNav}>
          <Menu className="h-5 w-5" />
          <span className="sr-only">Toggle menu</span>
        </Button>
      </div>
    </header>
  );
}
