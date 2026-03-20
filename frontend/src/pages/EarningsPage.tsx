import { DollarSign, Tag, TrendingUp, Ticket } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { EmptyState } from '@/components/common/EmptyState';
import { useEarnings } from '@/hooks/use-seller';

/**
 * Seller earnings dashboard showing summary stats and recent sales.
 */
export function EarningsPage() {
  const { data: earnings, isLoading } = useEarnings();

  if (isLoading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-6 sm:px-6 lg:px-8">
        <Skeleton className="mb-6 h-8 w-48" />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-32 w-full" />
          ))}
        </div>
        <Skeleton className="mt-8 h-64 w-full" />
      </div>
    );
  }

  if (!earnings) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-6 sm:px-6 lg:px-8">
        <EmptyState
          icon={DollarSign}
          title="No earnings data"
          description="Start selling tickets to see your earnings here."
          action={{ label: 'Sell Tickets', href: '/sell' }}
        />
      </div>
    );
  }

  const stats = [
    {
      title: 'Total Earnings',
      value: `$${earnings.totalEarnings.toFixed(2)}`,
      icon: DollarSign,
      description: `${earnings.totalListings} total listings`,
    },
    {
      title: 'Active Listings',
      value: earnings.activeListings.toString(),
      icon: Tag,
      description: 'Currently for sale',
    },
    {
      title: 'Sold Listings',
      value: earnings.soldListings.toString(),
      icon: TrendingUp,
      description: 'Successfully sold',
    },
  ];

  return (
    <div className="mx-auto max-w-5xl px-4 py-6 sm:px-6 lg:px-8">
      <h1 className="mb-6 text-2xl font-bold">Earnings</h1>

      {/* Summary Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {stats.map((stat) => (
          <Card key={stat.title}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                {stat.title}
              </CardTitle>
              <stat.icon className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stat.value}</div>
              <p className="mt-1 text-xs text-muted-foreground">{stat.description}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Recent Sales */}
      <div className="mt-8">
        <h2 className="mb-4 text-lg font-semibold">Recent Sales</h2>
        {earnings.recentSales.length === 0 ? (
          <EmptyState
            icon={Ticket}
            title="No sales yet"
            description="When your tickets sell, the sales will appear here."
          />
        ) : (
          <>
            {/* Mobile: Card layout */}
            <div className="space-y-3 md:hidden">
              {earnings.recentSales.map((sale) => (
                <div key={sale.orderId} className="rounded-lg border p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <p className="font-medium">{sale.eventName}</p>
                      <p className="mt-1 text-sm text-muted-foreground">
                        {sale.sectionName} &middot; {sale.seatInfo}
                      </p>
                    </div>
                    <span className="shrink-0 text-lg font-semibold">
                      ${sale.pricePaid.toFixed(2)}
                    </span>
                  </div>
                  <p className="mt-2 text-xs text-muted-foreground">
                    Sold {new Date(sale.soldAt).toLocaleDateString()}
                  </p>
                </div>
              ))}
            </div>

            {/* Desktop: Table layout */}
            <div className="hidden md:block">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Event</TableHead>
                    <TableHead>Section</TableHead>
                    <TableHead>Seat</TableHead>
                    <TableHead>Price Paid</TableHead>
                    <TableHead>Sold</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {earnings.recentSales.map((sale) => (
                    <TableRow key={sale.orderId}>
                      <TableCell className="font-medium">{sale.eventName}</TableCell>
                      <TableCell>{sale.sectionName}</TableCell>
                      <TableCell>{sale.seatInfo}</TableCell>
                      <TableCell>${sale.pricePaid.toFixed(2)}</TableCell>
                      <TableCell>{new Date(sale.soldAt).toLocaleDateString()}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
