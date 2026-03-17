import { Heart } from 'lucide-react';
import { EventCard } from '@/components/events/EventCard';
import { EmptyState } from '@/components/common/EmptyState';
import { Skeleton } from '@/components/ui/skeleton';
import { useFavorites } from '@/hooks/use-favorites';
import { ROUTES } from '@/lib/constants';

/**
 * Page displaying the user's favorited events in a responsive grid.
 * Shows an empty state when there are no favorites.
 */
export function FavoritesPage() {
  const { data: favorites, isLoading } = useFavorites();

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="mb-6 text-2xl font-bold tracking-tight sm:text-3xl">My Favorites</h1>

      {isLoading ? (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <EventCardSkeleton key={i} />
          ))}
        </div>
      ) : favorites && favorites.length > 0 ? (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {favorites.map((favorite) => (
            <EventCard key={favorite.id} event={favorite.event} />
          ))}
        </div>
      ) : (
        <EmptyState
          icon={Heart}
          title="No favorites yet"
          description="Browse events and tap the heart icon to save your favorites here."
          action={{ label: 'Browse Events', href: ROUTES.EVENTS }}
        />
      )}
    </div>
  );
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
