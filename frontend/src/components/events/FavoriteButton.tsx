import { Heart } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/stores/auth-store';
import { useCheckFavorite, useAddFavorite, useRemoveFavorite } from '@/hooks/use-favorites';

interface FavoriteButtonProps {
  eventId?: number;
  className?: string;
}

/**
 * Heart icon toggle button for favoriting events.
 * Shows a filled red heart when the event is favorited.
 * Disabled when the user is not authenticated.
 */
export function FavoriteButton({ eventId, className }: FavoriteButtonProps) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const { data: isFavorited } = useCheckFavorite(eventId ?? 0);
  const addFavorite = useAddFavorite();
  const removeFavorite = useRemoveFavorite();

  const handleToggle = () => {
    if (!eventId) return;
    if (isFavorited) {
      removeFavorite.mutate(eventId);
    } else {
      addFavorite.mutate(eventId);
    }
  };

  const isLoading = addFavorite.isPending || removeFavorite.isPending;

  return (
    <Button
      variant="ghost"
      size="icon"
      className={cn('group', className)}
      onClick={handleToggle}
      disabled={!isAuthenticated || !eventId || isLoading}
      aria-label={isFavorited ? 'Remove from favorites' : 'Add to favorites'}
    >
      <Heart
        className={cn(
          'h-5 w-5 transition-colors',
          isFavorited
            ? 'fill-red-500 text-red-500'
            : 'text-muted-foreground group-hover:text-red-400',
        )}
      />
    </Button>
  );
}
