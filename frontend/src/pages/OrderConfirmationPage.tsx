import { Link, useParams } from 'react-router';
import { CheckCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { OrderItemRow } from '@/components/orders/OrderItemRow';
import { useOrder } from '@/hooks/use-orders';
import { formatCurrency, formatDate } from '@/lib/formatters';
import { ROUTES } from '@/lib/constants';

/**
 * Order confirmation page shown after a successful checkout,
 * and also used as the order detail view from order history.
 */
export function OrderConfirmationPage() {
  const { orderNumber } = useParams<{ orderNumber: string }>();
  const { data: order, isLoading } = useOrder(orderNumber ?? '');

  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="space-y-6">
          <Skeleton className="mx-auto h-16 w-16 rounded-full" />
          <Skeleton className="mx-auto h-8 w-64" />
          <Skeleton className="mx-auto h-5 w-48" />
          <Skeleton className="h-48 w-full" />
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="flex min-h-[calc(100vh-10rem)] items-center justify-center">
        <div className="text-center">
          <h1 className="text-3xl font-bold">Order Not Found</h1>
          <p className="mt-2 text-muted-foreground">
            The order you are looking for does not exist.
          </p>
          <Button className="mt-4" asChild>
            <Link to={ROUTES.ORDERS}>View Orders</Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-6 sm:px-6 lg:px-8">
      {/* Success Header */}
      <div className="mb-8 text-center">
        <CheckCircle className="mx-auto h-16 w-16 text-emerald-500" />
        <h1 className="mt-4 text-2xl font-bold">
          {order.status === 'CONFIRMED' ? 'Order Confirmed!' : 'Order Details'}
        </h1>
        <p className="mt-1 text-muted-foreground">
          Order <span className="font-mono font-semibold">#{order.orderNumber}</span>
        </p>
        <Badge
          variant="secondary"
          className={
            order.status === 'CONFIRMED'
              ? 'mt-2 bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400'
              : 'mt-2'
          }
        >
          {order.status}
        </Badge>
      </div>

      {/* Order Details */}
      <div className="space-y-4">
        <div className="flex justify-between text-sm text-muted-foreground">
          <span>Order Date</span>
          <span>{formatDate(order.createdAt)}</span>
        </div>
        {order.confirmedAt && (
          <div className="flex justify-between text-sm text-muted-foreground">
            <span>Confirmed</span>
            <span>{formatDate(order.confirmedAt)}</span>
          </div>
        )}
        <div className="flex justify-between text-sm text-muted-foreground">
          <span>Payment Method</span>
          <span>{order.paymentMethod}</span>
        </div>
      </div>

      <Separator className="my-6" />

      {/* Items */}
      <h2 className="mb-3 text-lg font-semibold">Tickets ({order.items.length})</h2>
      <div className="space-y-2">
        {order.items.map((item) => (
          <OrderItemRow
            key={item.id}
            item={item}
            orderNumber={order.orderNumber}
            orderStatus={order.status}
          />
        ))}
      </div>

      <Separator className="my-6" />

      {/* Price Breakdown */}
      <div className="space-y-2 text-sm">
        <div className="flex justify-between">
          <span className="text-muted-foreground">Subtotal</span>
          <span>{formatCurrency(order.subtotal)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-muted-foreground">Service Fee</span>
          <span>{formatCurrency(order.serviceFee)}</span>
        </div>
        <Separator className="my-2" />
        <div className="flex justify-between text-base font-semibold">
          <span>Total</span>
          <span>{formatCurrency(order.total)}</span>
        </div>
      </div>

      {/* Actions */}
      <div className="mt-8 flex flex-col gap-3 sm:flex-row">
        <Button className="flex-1" asChild>
          <Link to={ROUTES.ORDERS}>View Order History</Link>
        </Button>
        <Button variant="outline" className="flex-1" asChild>
          <Link to={ROUTES.EVENTS}>Continue Browsing</Link>
        </Button>
      </div>
    </div>
  );
}
