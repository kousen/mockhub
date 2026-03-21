import { useState } from 'react';
import { useParams } from 'react-router';
import { Calendar, Clock, Map, MapPin, Tag, Ticket, Users } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Skeleton } from '@/components/ui/skeleton';
import { Separator } from '@/components/ui/separator';
import { PricePredictionBadge } from '@/components/ai/PricePredictionBadge';
import { FavoriteButton } from '@/components/events/FavoriteButton';
import { Button } from '@/components/ui/button';
import { TicketListView } from '@/components/tickets/TicketListView';
import { VenueMap } from '@/components/tickets/VenueMap';
import { PriceHistoryChart } from '@/components/tickets/PriceHistoryChart';
import {
  useEvent,
  useEventListings,
  useEventPriceHistory,
  useEventSections,
} from '@/hooks/use-events';
import { formatCurrency, formatDate } from '@/lib/formatters';

function DetailSkeleton() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <Skeleton className="mb-6 h-64 w-full rounded-lg" />
      <div className="space-y-4">
        <Skeleton className="h-8 w-2/3" />
        <Skeleton className="h-5 w-1/3" />
        <div className="flex gap-4">
          <Skeleton className="h-5 w-40" />
          <Skeleton className="h-5 w-40" />
        </div>
      </div>
      <Skeleton className="mt-8 h-[400px] w-full rounded-lg" />
    </div>
  );
}

/**
 * Event detail page showing full event info, ticket listings,
 * section availability, and price history chart.
 */
export function EventDetailPage() {
  const { slug } = useParams<{ slug: string }>();
  const eventSlug = slug ?? '';

  const [activeTab, setActiveTab] = useState('tickets');
  const [selectedSectionId, setSelectedSectionId] = useState<number | null>(null);

  const { data: event, isLoading: eventLoading, error: eventError } = useEvent(eventSlug);
  const { data: listings, isLoading: listingsLoading } = useEventListings(eventSlug);
  const { data: priceHistory, isLoading: priceHistoryLoading } = useEventPriceHistory(eventSlug);
  const { data: sections, isLoading: sectionsLoading } = useEventSections(eventSlug);

  if (eventLoading) {
    return <DetailSkeleton />;
  }

  if (eventError) {
    return (
      <div className="flex min-h-[calc(100vh-10rem)] items-center justify-center">
        <div className="text-center">
          <h1 className="text-3xl font-bold">Error Loading Event</h1>
          <p className="mt-2 text-muted-foreground">
            {eventError.message || 'Something went wrong. Please try again.'}
          </p>
        </div>
      </div>
    );
  }

  if (!event) {
    return (
      <div className="flex min-h-[calc(100vh-10rem)] items-center justify-center">
        <div className="text-center">
          <h1 className="text-3xl font-bold">Event Not Found</h1>
          <p className="mt-2 text-muted-foreground">
            The event you are looking for does not exist or has been removed.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      {/* Hero / Image Area */}
      <div className="relative mb-6 flex h-48 items-center justify-center overflow-hidden rounded-lg bg-gradient-to-br from-primary/20 via-primary/10 to-background sm:h-64 md:h-80">
        {event.primaryImageUrl ? (
          <img
            src={event.primaryImageUrl}
            alt={event.name}
            loading="lazy"
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="flex flex-col items-center gap-2 text-center">
            <Ticket className="h-12 w-12 text-primary/50" />
            <span className="max-w-md text-lg font-medium text-muted-foreground/70">
              {event.name}
            </span>
          </div>
        )}
        <FavoriteButton className="absolute right-4 top-4" />
      </div>

      {/* Event Header */}
      <div className="mb-6">
        <div className="flex flex-wrap items-start gap-3">
          <h1 className="text-2xl font-bold tracking-tight sm:text-3xl lg:text-4xl">
            {event.name}
          </h1>
          <Badge className="mt-1">{event.category.name}</Badge>
          {event.status !== 'ON_SALE' && (
            <Badge variant="destructive" className="mt-1">
              {event.status}
            </Badge>
          )}
        </div>

        {event.artistName && (
          <p className="mt-2 text-lg text-muted-foreground">{event.artistName}</p>
        )}

        {event.description && (
          <p className="mt-3 text-sm text-muted-foreground">{event.description}</p>
        )}

        {/* Event Meta */}
        <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-sm text-muted-foreground">
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 shrink-0" />
            {formatDate(event.eventDate)}
          </div>
          {event.doorsOpenAt && (
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 shrink-0" />
              Doors: {formatDate(event.doorsOpenAt)}
            </div>
          )}
          <div className="flex items-center gap-2">
            <MapPin className="h-4 w-4 shrink-0" />
            {event.venue.name}, {event.venue.city}, {event.venue.state}
          </div>
          <div className="flex items-center gap-2">
            <Users className="h-4 w-4 shrink-0" />
            {event.availableTickets} of {event.totalTickets} available
          </div>
        </div>

        {/* Tags */}
        {event.tags.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-1.5">
            {event.tags.map((tag) => (
              <Badge key={tag.id} variant="outline" className="text-xs">
                <Tag className="mr-1 h-3 w-3" />
                {tag.name}
              </Badge>
            ))}
          </div>
        )}

        {/* AI Price Prediction */}
        <div className="mt-4">
          <PricePredictionBadge slug={eventSlug} />
        </div>

        {/* Price Summary */}
        <div className="mt-4 flex flex-wrap gap-4 rounded-lg border p-4">
          <div>
            <p className="text-xs text-muted-foreground">Base Price</p>
            <p className="text-lg font-semibold">{formatCurrency(event.basePrice)}</p>
          </div>
          {event.minPrice !== null && (
            <div>
              <p className="text-xs text-muted-foreground">From</p>
              <p className="text-lg font-semibold text-emerald-600 dark:text-emerald-400">
                {formatCurrency(event.minPrice)}
              </p>
            </div>
          )}
          {event.maxPrice !== null && (
            <div>
              <p className="text-xs text-muted-foreground">Up to</p>
              <p className="text-lg font-semibold">{formatCurrency(event.maxPrice)}</p>
            </div>
          )}
        </div>
      </div>

      <Separator className="mb-6" />

      {/* Tabs: Tickets, Venue Map, Price History */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="mb-4">
          <TabsTrigger value="tickets">
            <Ticket className="mr-1.5 h-4 w-4" />
            Tickets
            {listings && (
              <Badge variant="secondary" className="ml-1.5 text-xs">
                {listings.length}
              </Badge>
            )}
          </TabsTrigger>
          <TabsTrigger value="venue-map">
            <Map className="mr-1.5 h-4 w-4" />
            Venue Map
          </TabsTrigger>
          <TabsTrigger value="price-history">Price History</TabsTrigger>
        </TabsList>

        <TabsContent value="tickets">
          <TicketListView
            listings={listings ?? []}
            isLoading={listingsLoading}
            sectionFilter={
              selectedSectionId
                ? ((sections ?? []).find((s) => s.sectionId === selectedSectionId)?.sectionName ??
                  null)
                : null
            }
            onClearFilter={() => setSelectedSectionId(null)}
          />
        </TabsContent>

        <TabsContent value="venue-map">
          <VenueMap
            sections={sections ?? []}
            selectedSectionId={selectedSectionId}
            onSectionSelect={setSelectedSectionId}
            isLoading={sectionsLoading}
          />
          {selectedSectionId && (
            <div className="mt-4 flex justify-center">
              <Button onClick={() => setActiveTab('tickets')}>
                <Ticket className="mr-1.5 h-4 w-4" />
                View {
                  (sections ?? []).find((s) => s.sectionId === selectedSectionId)?.sectionName
                }{' '}
                tickets
              </Button>
            </div>
          )}
        </TabsContent>

        <TabsContent value="price-history">
          <PriceHistoryChart data={priceHistory ?? []} isLoading={priceHistoryLoading} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
