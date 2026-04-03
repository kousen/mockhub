import { useMemo, useState } from 'react';
import { ArrowUpDown, Loader2, ShoppingCart, X } from 'lucide-react';
import { toast } from 'sonner';
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
import { useAddToCart } from '@/hooks/use-cart';
import { useAuthStore } from '@/stores/auth-store';
import { useCartStore } from '@/stores/cart-store';
import type { Listing } from '@/types/ticket';

interface TicketListViewProps {
  listings: Listing[];
  isLoading?: boolean;
  sectionFilter?: string | null;
  onClearFilter?: () => void;
}

type SortField = 'price' | 'section';
type SortDirection = 'asc' | 'desc';

/**
 * Table view of available ticket listings for an event.
 * Supports sorting by price and section. "Add to Cart" buttons
 * allow authenticated users to add tickets to their cart.
 */
export function TicketListView({
  listings,
  isLoading,
  sectionFilter,
  onClearFilter,
}: TicketListViewProps) {
  const [sortField, setSortField] = useState<SortField>('section');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [addingListingId, setAddingListingId] = useState<number | null>(null);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const openDrawer = useCartStore((state) => state.openDrawer);
  const addToCart = useAddToCart();

  const handleAddToCart = (listingId: number) => {
    setAddingListingId(listingId);
    addToCart.mutate(
      { listingId },
      {
        onSuccess: () => {
          toast.success('Added to cart!');
          openDrawer();
          setAddingListingId(null);
        },
        onError: () => {
          toast.error('Failed to add to cart. Please try again.');
          setAddingListingId(null);
        },
      },
    );
  };

  const filteredListings = useMemo(() => {
    if (!sectionFilter) return listings;
    return listings.filter((listing) => listing.sectionName === sectionFilter);
  }, [listings, sectionFilter]);

  const sortedListings = useMemo(() => {
    const sorted = [...filteredListings];
    sorted.sort((a, b) => {
      let cmp = 0;
      if (sortField === 'price') {
        cmp = a.computedPrice - b.computedPrice;
      } else if (sortField === 'section') {
        cmp = a.sectionName.localeCompare(b.sectionName);
        if (cmp === 0) {
          cmp = (a.rowLabel ?? '').localeCompare(b.rowLabel ?? '');
        }
        if (cmp === 0) {
          cmp = Number(a.seatNumber ?? 0) - Number(b.seatNumber ?? 0);
        }
      }
      return sortDirection === 'desc' ? -cmp : cmp;
    });
    return sorted;
  }, [filteredListings, sortField, sortDirection]);

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
          <Skeleton key={`skeleton-${index}`} className="h-12 w-full" />
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
      {sectionFilter && (
        <div className="mb-3 flex items-center gap-2">
          <Badge variant="secondary" className="gap-1 text-sm">
            {sectionFilter} — {filteredListings.length} ticket
            {filteredListings.length !== 1 ? 's' : ''}
            {onClearFilter && (
              <button
                onClick={onClearFilter}
                className="ml-1 rounded-full p-0.5 hover:bg-muted"
                aria-label="Clear section filter"
              >
                <X className="h-3 w-3" />
              </button>
            )}
          </Badge>
        </div>
      )}
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
                <PriceTag computedPrice={listing.computedPrice} listedPrice={listing.listedPrice} />
              </TableCell>
              <TableCell className="text-right">
                <Button
                  size="sm"
                  disabled={!isAuthenticated || addingListingId === listing.id}
                  title={isAuthenticated ? 'Add to cart' : 'Log in to add to cart'}
                  onClick={() => handleAddToCart(listing.id)}
                >
                  {addingListingId === listing.id ? (
                    <Loader2 className="mr-1 h-3 w-3 animate-spin" />
                  ) : (
                    <ShoppingCart className="mr-1 h-3 w-3" />
                  )}
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
