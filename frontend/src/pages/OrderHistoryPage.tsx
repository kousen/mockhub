import { useState } from 'react';
import { Package } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { OrderCard } from '@/components/orders/OrderCard';
import { EmptyState } from '@/components/common/EmptyState';
import { useOrders } from '@/hooks/use-orders';
import { ROUTES } from '@/lib/constants';

/**
 * Order history page showing a paginated list of the user's orders.
 * Each order card links to its confirmation/detail page.
 */
export function OrderHistoryPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useOrders(page);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-6 sm:px-6 lg:px-8">
        <Skeleton className="mb-6 h-8 w-48" />
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, index) => (
            <div key={`skeleton-${index}`} className="rounded-lg border p-4 space-y-2">
              <div className="flex items-center justify-between">
                <Skeleton className="h-4 w-32" />
                <Skeleton className="h-5 w-20 rounded-full" />
              </div>
              <Skeleton className="h-4 w-48" />
              <div className="flex items-center justify-between">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-4 w-16" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  const orders = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  if (orders.length === 0 && page === 0) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-6 sm:px-6 lg:px-8">
        <EmptyState
          icon={Package}
          title="No orders yet"
          description="When you purchase tickets, your orders will appear here."
          action={{ label: 'Browse Events', href: ROUTES.EVENTS }}
        />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-6 sm:px-6 lg:px-8">
      <h1 className="mb-6 text-2xl font-bold">My Orders</h1>

      <div className="space-y-3">
        {orders.map((order) => (
          <OrderCard key={order.id} order={order} />
        ))}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage((prev) => Math.max(0, prev - 1))}
            disabled={page === 0}
          >
            Previous
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage((prev) => prev + 1)}
            disabled={page >= totalPages - 1}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  );
}
