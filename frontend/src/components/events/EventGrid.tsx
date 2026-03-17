import { EventCard } from './EventCard';
import { Skeleton } from '@/components/ui/skeleton';
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
export function EventGrid({ events, isLoading }: EventGridProps) {
  if (isLoading) {
    return (
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {Array.from({ length: 8 }).map((_, index) => (
          <EventCardSkeleton key={index} />
        ))}
      </div>
    );
  }

  if (events.length === 0) {
    return (
      <div className="flex min-h-[200px] flex-col items-center justify-center rounded-lg border border-dashed p-8 text-center">
        <p className="text-lg font-medium text-muted-foreground">No events found</p>
        <p className="mt-1 text-sm text-muted-foreground">
          Try adjusting your search or filter criteria.
        </p>
      </div>
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
