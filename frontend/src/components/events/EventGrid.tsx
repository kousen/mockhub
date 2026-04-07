import { Search } from 'lucide-react';
import { EventCard } from './EventCard';
import { EmptyState } from '@/components/common/EmptyState';
import { Skeleton } from '@/components/ui/skeleton';
import { ROUTES } from '@/lib/constants';
import type { EventSummary } from '@/types/event';

interface EventGridProps {
  events: EventSummary[];
  isLoading?: boolean;
}

function EventCardSkeleton() {
  return (
    <div className="flex flex-col gap-6 rounded-xl border bg-card py-6 shadow-sm">
      <div className="px-6">
        <Skeleton className="mb-2 h-40 w-full rounded-md" />
        <Skeleton className="h-5 w-3/4" />
        <Skeleton className="mt-2 h-4 w-1/2" />
      </div>
      <div className="space-y-2 px-6">
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="h-4 w-1/2" />
      </div>
      <div className="flex items-center justify-between px-6">
        <Skeleton className="h-5 w-24" />
        <Skeleton className="h-8 w-24 rounded-md" />
      </div>
    </div>
  );
}

/**
 * Responsive grid for displaying event cards.
 * Shows skeleton placeholders while loading.
 */
export function EventGrid({ events, isLoading }: Readonly<EventGridProps>) {
  if (isLoading) {
    return (
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {Array.from({ length: 8 }, (_, i) => i).map((n) => (
          <EventCardSkeleton key={`skeleton-${n}`} />
        ))}
      </div>
    );
  }

  if (events.length === 0) {
    return (
      <EmptyState
        icon={Search}
        title="No events found"
        description="Try adjusting your search or filter criteria."
        action={{ label: 'View All Events', href: ROUTES.EVENTS }}
      />
    );
  }

  return (
    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {events.map((event) => (
        <EventCard key={event.id} event={event} />
      ))}
    </div>
  );
}
