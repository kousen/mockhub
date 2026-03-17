import { Link } from 'react-router';
import { ChevronRight } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import { formatCurrency, formatShortDate } from '@/lib/formatters';
import type { OrderSummary } from '@/types/order';

interface OrderCardProps {
  order: OrderSummary;
}

function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
  switch (status.toUpperCase()) {
    case 'CONFIRMED':
      return 'default';
    case 'PENDING':
      return 'secondary';
    case 'FAILED':
      return 'destructive';
    case 'CANCELLED':
      return 'outline';
    default:
      return 'secondary';
  }
}

function getStatusColor(status: string): string {
  switch (status.toUpperCase()) {
    case 'CONFIRMED':
      return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400';
    case 'PENDING':
      return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400';
    case 'FAILED':
      return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
    case 'CANCELLED':
      return 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400';
    default:
      return '';
  }
}

/**
 * Card displaying an order summary with status badge, date, total, and item count.
 * Links to the order detail/confirmation page.
 */
export function OrderCard({ order }: OrderCardProps) {
  const statusVariant = getStatusVariant(order.status);

  return (
    <Link to={`/orders/${order.orderNumber}/confirmation`}>
      <Card className="transition-colors hover:bg-accent/50">
        <CardContent className="flex items-center justify-between p-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="text-sm font-semibold">#{order.orderNumber}</span>
              <Badge variant={statusVariant} className={getStatusColor(order.status)}>
                {order.status}
              </Badge>
            </div>
            <p className="text-xs text-muted-foreground">
              {formatShortDate(order.createdAt)}
              {' \u00b7 '}
              {order.itemCount} {order.itemCount === 1 ? 'ticket' : 'tickets'}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold">{formatCurrency(order.total)}</span>
            <ChevronRight className="h-4 w-4 text-muted-foreground" />
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
