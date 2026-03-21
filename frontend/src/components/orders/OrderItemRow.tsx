import { Link } from 'react-router';
import { Download, Loader2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { formatCurrency } from '@/lib/formatters';
import { useDownloadTicket } from '@/hooks/use-orders';
import type { OrderItem } from '@/types/order';

interface OrderItemRowProps {
  item: OrderItem;
  orderNumber?: string;
  orderStatus?: string;
}

/**
 * Row displaying a single order item with event name link,
 * section/row/seat details, ticket type, price paid, and
 * a download button for confirmed orders.
 */
export function OrderItemRow({ item, orderNumber, orderStatus }: OrderItemRowProps) {
  const { mutate: downloadTicket, isPending } = useDownloadTicket();

  const canDownload = orderStatus === 'CONFIRMED' && orderNumber;

  const handleDownload = () => {
    if (orderNumber) {
      downloadTicket({ orderNumber, ticketId: item.ticketId });
    }
  };

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
      <div className="flex items-center gap-2">
        <span className="text-sm font-semibold">{formatCurrency(item.pricePaid)}</span>
        {canDownload && (
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={handleDownload}
            disabled={isPending}
            title="Download ticket PDF"
          >
            {isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Download className="h-4 w-4" />
            )}
          </Button>
        )}
      </div>
    </div>
  );
}
