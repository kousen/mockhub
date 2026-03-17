import { Heart } from 'lucide-react';
import { EventCard } from '@/components/events/EventCard';
import { Skeleton } from '@/components/ui/skeleton';
import { useFavorites } from '@/hooks/use-favorites';

/**
 * Page displaying the user's favorited events in a responsive grid.
 * Shows an empty state when there are no favorites.
 */
export function FavoritesPage() {
  const { data: favorites, isLoading } = useFavorites();

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="mb-6 text-3xl font-bold">My Favorites</h1>

      {isLoading ? (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-80 rounded-lg" />
          ))}
        </div>
      ) : favorites && favorites.length > 0 ? (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {favorites.map((favorite) => (
            <EventCard key={favorite.id} event={favorite.event} />
          ))}
        </div>
      ) : (
        <div className="flex min-h-[40vh] flex-col items-center justify-center text-center">
          <Heart className="mb-4 h-16 w-16 text-muted-foreground/30" />
          <h2 className="text-xl font-semibold">No favorites yet</h2>
          <p className="mt-2 max-w-sm text-muted-foreground">
            Browse events and tap the heart icon to save your favorites here.
          </p>
        </div>
      )}
    </div>
  );
}
