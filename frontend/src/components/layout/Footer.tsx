import { Link } from 'react-router';
import { Ticket } from 'lucide-react';
import { Separator } from '@/components/ui/separator';
import { APP_NAME, ROUTES } from '@/lib/constants';

export function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="border-t border-border bg-background">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="flex flex-col items-center gap-4 md:flex-row md:justify-between">
          <div className="flex items-center gap-2">
            <Ticket className="h-5 w-5 text-primary" />
            <span className="text-sm font-semibold">{APP_NAME}</span>
            <span className="text-xs text-muted-foreground">- A Teaching Platform</span>
          </div>
          <nav className="flex gap-6">
            <Link
              to={ROUTES.EVENTS}
              className="text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
              Events
            </Link>
            <Link
              to={ROUTES.HOME}
              className="text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
              About
            </Link>
          </nav>
        </div>
        <Separator className="my-4" />
        <p className="text-center text-xs text-muted-foreground">
          &copy; {currentYear} {APP_NAME}. Built for learning. All rights reserved.
        </p>
      </div>
    </footer>
  );
}
