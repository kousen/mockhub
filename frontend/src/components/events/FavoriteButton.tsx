import { useState } from 'react';
import { Heart } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface FavoriteButtonProps {
  className?: string;
}

/**
 * Heart icon toggle button for favoriting events.
 * Currently just local UI state -- Wave 4 will wire this to the API.
 */
export function FavoriteButton({ className }: FavoriteButtonProps) {
  const [isFavorited, setIsFavorited] = useState(false);

  return (
    <Button
      variant="ghost"
      size="icon"
      className={cn('group', className)}
      onClick={() => setIsFavorited((prev) => !prev)}
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
