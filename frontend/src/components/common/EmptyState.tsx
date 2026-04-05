import { Link } from 'react-router';
import { Button } from '@/components/ui/button';
import type { LucideIcon } from 'lucide-react';

interface EmptyStateAction {
  label: string;
  href: string;
}

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: EmptyStateAction;
}

/**
 * Reusable empty state component for pages with no data.
 * Displays an icon, title, description, and an optional action button.
 */
export function EmptyState({ icon: Icon, title, description, action }: Readonly<EmptyStateProps>) {
  return (
    <div className="flex min-h-[40vh] flex-col items-center justify-center gap-4 text-center">
      <Icon className="h-16 w-16 text-muted-foreground/30" />
      <div>
        <h2 className="text-xl font-semibold">{title}</h2>
        <p className="mt-2 max-w-sm text-muted-foreground">{description}</p>
      </div>
      {action && (
        <Button asChild>
          <Link to={action.href}>{action.label}</Link>
        </Button>
      )}
    </div>
  );
}
