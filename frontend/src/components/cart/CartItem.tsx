import { Link } from 'react-router';
import { X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { formatCurrency } from '@/lib/formatters';
import type { CartItem as CartItemType } from '@/types/cart';

interface CartItemProps {
  item: CartItemType;
  onRemove: (itemId: number) => void;
  isRemoving?: boolean;
  readOnly?: boolean;
}

/**
 * Individual cart item row showing event name, section/row/seat info,
 * ticket type badge, price, and a remove button.
 * Highlights price changes since the item was added.
 */
export function CartItem({ item, onRemove, isRemoving, readOnly }: CartItemProps) {
  const priceChanged = item.currentPrice !== item.priceAtAdd;
  const priceDiff = item.currentPrice - item.priceAtAdd;

  return (
    <div className="flex items-start gap-3 rounded-lg border p-3">
      <div className="flex-1 min-w-0">
        <Link
          to={`/events/${item.eventSlug}`}
          className="text-sm font-medium hover:underline line-clamp-1"
        >
          {item.eventName}
        </Link>
        <p className="mt-0.5 text-xs text-muted-foreground">
          {item.sectionName}
          {item.rowLabel && ` / Row ${item.rowLabel}`}
          {item.seatNumber && ` / Seat ${item.seatNumber}`}
        </p>
        <div className="mt-1.5 flex items-center gap-2">
          <Badge variant="secondary" className="text-xs">
            {item.ticketType}
          </Badge>
          {priceChanged && (
            <span
              className={`text-xs font-medium ${priceDiff > 0 ? 'text-red-600 dark:text-red-400' : 'text-emerald-600 dark:text-emerald-400'}`}
            >
              {priceDiff > 0 ? '+' : ''}
              {formatCurrency(priceDiff)} since added
            </span>
          )}
        </div>
      </div>
      <div className="flex flex-col items-end gap-1">
        <span className="text-sm font-semibold">{formatCurrency(item.currentPrice)}</span>
        {!readOnly && (
          <Button
            variant="ghost"
            size="icon"
            className="h-6 w-6"
            onClick={() => onRemove(item.id)}
            disabled={isRemoving}
            aria-label={`Remove ${item.eventName} from cart`}
          >
            <X className="h-3.5 w-3.5" />
          </Button>
        )}
      </div>
    </div>
  );
}
