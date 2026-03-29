import { useState } from 'react';
import { useParams } from 'react-router';
import { Calendar, Clock, ExternalLink, Map, MapPin, Tag, Ticket, Users } from 'lucide-react';
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
import { useSpotifyArtist } from '@/hooks/use-spotify';
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
  const { data: spotifyArtist } = useSpotifyArtist(event?.spotifyArtistId ?? null);

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

        {/* Spotify Section */}
        {event.spotifyArtistId && (
          <div className="mt-4 space-y-3">
            {/* Genre tags from Spotify API */}
            {spotifyArtist?.genres && spotifyArtist.genres.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {spotifyArtist.genres.map((genre) => (
                  <Badge key={genre} variant="secondary" className="text-xs capitalize">
                    {genre}
                  </Badge>
                ))}
              </div>
            )}

            {/* Spotify embedded player */}
            <div className="overflow-hidden rounded-xl">
              <iframe
                src={`https://open.spotify.com/embed/artist/${encodeURIComponent(event.spotifyArtistId)}?utm_source=generator`}
                width="100%"
                height="352"
                frameBorder="0"
                allowFullScreen
                allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture"
                loading="lazy"
                title={`Spotify player for ${event.artistName || event.name}`}
              />
            </div>
            <p className="text-xs text-muted-foreground">
              Playback is not available in the embedded player due to browser privacy restrictions.
              Use the link below to listen on Spotify.
            </p>

            {/* Listen on Spotify link */}
            <a
              href={`https://open.spotify.com/artist/${encodeURIComponent(event.spotifyArtistId)}`}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-full bg-[#1DB954] px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-[#1ed760]"
            >
              <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2zm.12-3.36C15.24 8.4 8.82 8.16 5.16 9.301c-.6.179-1.2-.181-1.38-.721-.18-.601.18-1.2.72-1.381 4.26-1.26 11.28-1.02 15.721 1.621.539.3.719 1.02.419 1.56-.299.421-1.02.599-1.559.3z" />
              </svg>
              Open in Spotify
              <ExternalLink className="h-3 w-3" />
            </a>
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
