import { useMemo, useState } from 'react';
import { ArrowUpDown, ShoppingCart } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { PriceTag } from './PriceTag';
import type { Listing } from '@/types/ticket';

interface TicketListViewProps {
  listings: Listing[];
  isLoading?: boolean;
}

type SortField = 'price' | 'section';
type SortDirection = 'asc' | 'desc';

/**
 * Table view of available ticket listings for an event.
 * Supports sorting by price and section. "Add to Cart" buttons
 * are visible but disabled until Wave 3.
 */
export function TicketListView({ listings, isLoading }: TicketListViewProps) {
  const [sortField, setSortField] = useState<SortField>('price');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');

  const sortedListings = useMemo(() => {
    const sorted = [...listings];
    sorted.sort((a, b) => {
      let cmp = 0;
      if (sortField === 'price') {
        cmp = a.computedPrice - b.computedPrice;
      } else if (sortField === 'section') {
        cmp = a.sectionName.localeCompare(b.sectionName);
      }
      return sortDirection === 'desc' ? -cmp : cmp;
    });
    return sorted;
  }, [listings, sortField, sortDirection]);

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, index) => (
          <Skeleton key={index} className="h-12 w-full" />
        ))}
      </div>
    );
  }

  if (listings.length === 0) {
    return (
      <div className="flex min-h-[200px] flex-col items-center justify-center rounded-lg border border-dashed p-8 text-center">
        <p className="text-lg font-medium text-muted-foreground">No tickets listed</p>
        <p className="mt-1 text-sm text-muted-foreground">
          Check back later for available tickets.
        </p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>
              <Button
                variant="ghost"
                size="xs"
                onClick={() => toggleSort('section')}
                className="-ml-2"
              >
                Section
                <ArrowUpDown className="ml-1 h-3 w-3" />
              </Button>
            </TableHead>
            <TableHead>Row</TableHead>
            <TableHead>Seat</TableHead>
            <TableHead>Type</TableHead>
            <TableHead>
              <Button
                variant="ghost"
                size="xs"
                onClick={() => toggleSort('price')}
                className="-ml-2"
              >
                Price
                <ArrowUpDown className="ml-1 h-3 w-3" />
              </Button>
            </TableHead>
            <TableHead className="text-right">Action</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sortedListings.map((listing) => (
            <TableRow key={listing.id}>
              <TableCell className="font-medium">{listing.sectionName}</TableCell>
              <TableCell>{listing.rowLabel ?? '-'}</TableCell>
              <TableCell>{listing.seatNumber ?? 'GA'}</TableCell>
              <TableCell>
                <Badge variant="secondary" className="text-xs">
                  {listing.ticketType}
                </Badge>
              </TableCell>
              <TableCell>
                <PriceTag
                  computedPrice={listing.computedPrice}
                  listedPrice={listing.listedPrice}
                />
              </TableCell>
              <TableCell className="text-right">
                <Button size="sm" disabled title="Coming soon">
                  <ShoppingCart className="mr-1 h-3 w-3" />
                  Add to Cart
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
