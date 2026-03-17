import { Link } from 'react-router';
import { Calendar, MapPin, Ticket } from 'lucide-react';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { formatCurrency, formatShortDate } from '@/lib/formatters';
import type { EventSummary } from '@/types/event';

interface EventCardProps {
  event: EventSummary;
}

/**
 * Generates a deterministic gradient background based on the event name.
 * Used as a placeholder when no image is available.
 */
function getPlaceholderGradient(name: string): string {
  const gradients = [
    'from-violet-500/20 to-purple-600/20',
    'from-blue-500/20 to-indigo-600/20',
    'from-emerald-500/20 to-teal-600/20',
    'from-orange-500/20 to-red-600/20',
    'from-pink-500/20 to-rose-600/20',
    'from-cyan-500/20 to-blue-600/20',
    'from-amber-500/20 to-yellow-600/20',
    'from-fuchsia-500/20 to-purple-600/20',
  ];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return gradients[Math.abs(hash) % gradients.length];
}

export function EventCard({ event }: EventCardProps) {
  const eventUrl = `/events/${event.slug}`;

  return (
    <Card className="transition-shadow hover:shadow-lg">
      <CardHeader>
        <Link to={eventUrl} className="block">
          <div
            className={`mb-2 flex h-40 items-center justify-center rounded-md bg-gradient-to-br ${getPlaceholderGradient(event.name)}`}
          >
            {event.primaryImageUrl ? (
              <img
                src={event.primaryImageUrl}
                alt={event.name}
                className="h-full w-full rounded-md object-cover"
              />
            ) : (
              <div className="flex flex-col items-center gap-2">
                <Ticket className="h-10 w-10 text-muted-foreground/50" />
                <span className="max-w-[80%] text-center text-xs text-muted-foreground/70">
                  {event.name}
                </span>
              </div>
            )}
          </div>
        </Link>
        <div className="flex items-start justify-between gap-2">
          <Link to={eventUrl}>
            <CardTitle className="text-lg leading-tight hover:text-primary">
              {event.name}
            </CardTitle>
          </Link>
          <Badge variant="secondary" className="shrink-0 text-xs">
            {event.categoryName}
          </Badge>
        </div>
        {event.artistName && (
          <p className="text-sm text-muted-foreground">{event.artistName}</p>
        )}
      </CardHeader>
      <CardContent className="space-y-2">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <MapPin className="h-4 w-4 shrink-0" />
          <span className="truncate">
            {event.venueName}, {event.city}
          </span>
        </div>
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Calendar className="h-4 w-4 shrink-0" />
          {formatShortDate(event.eventDate)}
        </div>
      </CardContent>
      <CardFooter className="flex items-center justify-between">
        <div className="flex flex-col">
          {event.minPrice !== null ? (
            <span className="text-sm font-semibold">
              From {formatCurrency(event.minPrice)}
            </span>
          ) : (
            <span className="text-sm text-muted-foreground">No listings</span>
          )}
          <span className="text-xs text-muted-foreground">
            {event.availableTickets} ticket{event.availableTickets !== 1 ? 's' : ''} available
          </span>
        </div>
        <Button size="sm" variant="outline" asChild>
          <Link to={eventUrl}>View Tickets</Link>
        </Button>
      </CardFooter>
    </Card>
  );
}
