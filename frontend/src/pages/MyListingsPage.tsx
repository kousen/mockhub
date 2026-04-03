import { useState, useCallback } from 'react';
import { Link } from 'react-router';
import { toast } from 'sonner';
import { Loader2, Tag, Ticket, Calendar, MapPin, Pencil, X } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
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
import { useMyListings, useUpdatePrice, useDeactivateListing } from '@/hooks/use-seller';
import type { SellerListing } from '@/types/seller';

type StatusFilter = 'ALL' | 'ACTIVE' | 'SOLD' | 'CANCELLED';

function statusBadgeVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
  switch (status) {
    case 'ACTIVE':
      return 'default';
    case 'SOLD':
      return 'secondary';
    case 'CANCELLED':
      return 'destructive';
    default:
      return 'outline';
  }
}

function formatSeatInfo(listing: SellerListing): string {
  const parts = [listing.sectionName];
  if (listing.rowLabel) parts.push(`Row ${listing.rowLabel}`);
  if (listing.seatNumber) parts.push(`Seat ${listing.seatNumber}`);
  return parts.join(' \u00b7 ');
}

/**
 * Inline price editor shown when a seller clicks "Edit Price" on an active listing.
 */
function PriceEditor({ listing, onClose }: { listing: SellerListing; onClose: () => void }) {
  const [newPrice, setNewPrice] = useState(listing.listedPrice.toFixed(2));
  const updatePrice = useUpdatePrice();

  const handleSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      const price = Number.parseFloat(newPrice);
      if (Number.isNaN(price) || price <= 0) {
        toast.error('Please enter a valid price.');
        return;
      }
      updatePrice.mutate(
        { id: listing.id, request: { price } },
        {
          onSuccess: () => {
            toast.success('Price updated.');
            onClose();
          },
          onError: () => {
            toast.error('Failed to update price.');
          },
        },
      );
    },
    [listing.id, newPrice, updatePrice, onClose],
  );

  return (
    <form onSubmit={handleSubmit} className="flex items-center gap-2">
      <Input
        type="number"
        min="0.01"
        step="0.01"
        value={newPrice}
        onChange={(e) => setNewPrice(e.target.value)}
        className="h-8 w-24"
        autoFocus
      />
      <Button type="submit" size="sm" disabled={updatePrice.isPending}>
        {updatePrice.isPending ? <Loader2 className="h-3 w-3 animate-spin" /> : 'Save'}
      </Button>
      <Button type="button" variant="ghost" size="sm" onClick={onClose}>
        <X className="h-3 w-3" />
      </Button>
    </form>
  );
}

/**
 * Card view for a single listing, used on mobile screens.
 */
function ListingCard({
  listing,
  editingId,
  onEdit,
  onCancelEdit,
  onDeactivate,
  isDeactivating,
}: {
  listing: SellerListing;
  editingId: number | null;
  onEdit: (id: number) => void;
  onCancelEdit: () => void;
  onDeactivate: (id: number) => void;
  isDeactivating: boolean;
}) {
  return (
    <div className="rounded-lg border p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <Link to={`/events/${listing.eventSlug}`} className="font-medium hover:underline">
            {listing.eventName}
          </Link>
          <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
            <span className="flex items-center gap-1">
              <Calendar className="h-3.5 w-3.5" />
              {new Date(listing.eventDate).toLocaleDateString()}
            </span>
            <span className="flex items-center gap-1">
              <MapPin className="h-3.5 w-3.5" />
              {listing.venueName}
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">{formatSeatInfo(listing)}</p>
        </div>
        <Badge variant={statusBadgeVariant(listing.status)}>{listing.status}</Badge>
      </div>

      <div className="mt-3 flex items-center justify-between">
        {editingId === listing.id ? (
          <PriceEditor listing={listing} onClose={onCancelEdit} />
        ) : (
          <>
            <div>
              <span className="text-lg font-semibold">${listing.listedPrice.toFixed(2)}</span>
              {listing.computedPrice !== listing.listedPrice && (
                <span className="ml-2 text-sm text-muted-foreground">
                  (computed: ${listing.computedPrice.toFixed(2)})
                </span>
              )}
            </div>
            {listing.status === 'ACTIVE' && (
              <div className="flex items-center gap-2">
                <Button variant="ghost" size="sm" onClick={() => onEdit(listing.id)}>
                  <Pencil className="mr-1 h-3 w-3" />
                  Edit
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-destructive hover:text-destructive"
                  onClick={() => onDeactivate(listing.id)}
                  disabled={isDeactivating}
                >
                  {isDeactivating ? <Loader2 className="h-3 w-3 animate-spin" /> : 'Cancel'}
                </Button>
              </div>
            )}
          </>
        )}
      </div>
      <p className="mt-2 text-xs text-muted-foreground">
        Listed {new Date(listing.listedAt).toLocaleDateString()}
      </p>
    </div>
  );
}

/**
 * My Listings page showing a filterable list of the seller's ticket listings.
 * Uses a table layout on desktop and card layout on mobile.
 */
export function MyListingsPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [editingId, setEditingId] = useState<number | null>(null);
  const deactivateListing = useDeactivateListing();

  const queryStatus = statusFilter === 'ALL' ? undefined : statusFilter;
  const { data: listings, isLoading } = useMyListings(queryStatus);

  const handleDeactivate = useCallback(
    (id: number) => {
      deactivateListing.mutate(id, {
        onSuccess: () => {
          toast.success('Listing cancelled.');
        },
        onError: () => {
          toast.error('Failed to cancel listing.');
        },
      });
    },
    [deactivateListing],
  );

  if (isLoading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-6 sm:px-6 lg:px-8">
        <Skeleton className="mb-6 h-8 w-48" />
        <Skeleton className="mb-4 h-10 w-80" />
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, index) => (
            <Skeleton key={`skeleton-${index}`} className="h-20 w-full" />
          ))}
        </div>
      </div>
    );
  }

  const items = listings ?? [];

  return (
    <div className="mx-auto max-w-5xl px-4 py-6 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">My Listings</h1>
        <Button asChild>
          <Link to="/sell">
            <Tag className="mr-2 h-4 w-4" />
            Sell Tickets
          </Link>
        </Button>
      </div>

      <Tabs value={statusFilter} onValueChange={(value) => setStatusFilter(value as StatusFilter)}>
        <TabsList>
          <TabsTrigger value="ALL">All</TabsTrigger>
          <TabsTrigger value="ACTIVE">Active</TabsTrigger>
          <TabsTrigger value="SOLD">Sold</TabsTrigger>
          <TabsTrigger value="CANCELLED">Cancelled</TabsTrigger>
        </TabsList>

        <TabsContent value={statusFilter} className="mt-4">
          {items.length === 0 ? (
            <EmptyState
              icon={Ticket}
              title="No listings yet"
              description={
                statusFilter === 'ALL'
                  ? 'When you list tickets for sale, they will appear here.'
                  : `You have no ${statusFilter.toLowerCase()} listings.`
              }
              action={statusFilter === 'ALL' ? { label: 'Sell Tickets', href: '/sell' } : undefined}
            />
          ) : (
            <>
              {/* Mobile: Card layout */}
              <div className="space-y-3 md:hidden">
                {items.map((listing) => (
                  <ListingCard
                    key={listing.id}
                    listing={listing}
                    editingId={editingId}
                    onEdit={setEditingId}
                    onCancelEdit={() => setEditingId(null)}
                    onDeactivate={handleDeactivate}
                    isDeactivating={deactivateListing.isPending}
                  />
                ))}
              </div>

              {/* Desktop: Table layout */}
              <div className="hidden md:block">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Event</TableHead>
                      <TableHead>Date</TableHead>
                      <TableHead>Seat</TableHead>
                      <TableHead>Price</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Listed</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {items.map((listing) => (
                      <TableRow key={listing.id}>
                        <TableCell>
                          <Link
                            to={`/events/${listing.eventSlug}`}
                            className="font-medium hover:underline"
                          >
                            {listing.eventName}
                          </Link>
                        </TableCell>
                        <TableCell>{new Date(listing.eventDate).toLocaleDateString()}</TableCell>
                        <TableCell>{formatSeatInfo(listing)}</TableCell>
                        <TableCell>
                          {editingId === listing.id ? (
                            <PriceEditor listing={listing} onClose={() => setEditingId(null)} />
                          ) : (
                            <span>${listing.listedPrice.toFixed(2)}</span>
                          )}
                        </TableCell>
                        <TableCell>
                          <Badge variant={statusBadgeVariant(listing.status)}>
                            {listing.status}
                          </Badge>
                        </TableCell>
                        <TableCell>{new Date(listing.listedAt).toLocaleDateString()}</TableCell>
                        <TableCell className="text-right">
                          {listing.status === 'ACTIVE' && editingId !== listing.id && (
                            <div className="flex items-center justify-end gap-1">
                              <Button
                                variant="ghost"
                                size="sm"
                                aria-label="Edit price"
                                onClick={() => setEditingId(listing.id)}
                              >
                                <Pencil className="h-3 w-3" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                aria-label="Cancel listing"
                                className="text-destructive hover:text-destructive"
                                onClick={() => handleDeactivate(listing.id)}
                                disabled={deactivateListing.isPending}
                              >
                                {deactivateListing.isPending ? (
                                  <Loader2 className="h-3 w-3 animate-spin" />
                                ) : (
                                  <X className="h-3 w-3" />
                                )}
                              </Button>
                            </div>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
