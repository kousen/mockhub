import { Link } from 'react-router';
import { Badge } from '@/components/ui/badge';
import { formatCurrency } from '@/lib/formatters';
import type { OrderItem } from '@/types/order';

interface OrderItemRowProps {
  item: OrderItem;
}

/**
 * Row displaying a single order item with event name link,
 * section/row/seat details, ticket type, and price paid.
 */
export function OrderItemRow({ item }: OrderItemRowProps) {
  return (
    <div className="flex items-start justify-between gap-3 rounded-lg border p-3">
      <div className="min-w-0 flex-1">
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
        <Badge variant="secondary" className="mt-1.5 text-xs">
          {item.ticketType}
        </Badge>
      </div>
      <span className="text-sm font-semibold">{formatCurrency(item.pricePaid)}</span>
    </div>
  );
}
