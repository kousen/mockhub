import { useState } from 'react';
import { Link } from 'react-router';
import { Plus, Pencil, Trash2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { useAdminEvents, useDeleteEvent } from '@/hooks/use-admin';
import { formatCurrency, formatShortDate } from '@/lib/formatters';
import { ROUTES } from '@/lib/constants';

const statusVariants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  ACTIVE: 'default',
  DRAFT: 'secondary',
  CANCELLED: 'destructive',
  COMPLETED: 'outline',
};

/**
 * Admin events management page with data table, status badges,
 * ticket counts, and create/edit/delete actions.
 */
export function AdminEventsPage() {
  const [page, setPage] = useState(0);
  const { data: eventsPage, isLoading } = useAdminEvents(page, 20);
  const deleteEvent = useDeleteEvent();

  const events = eventsPage?.content ?? [];

  const handleDelete = (eventId: number, eventName: string) => {
    if (window.confirm(`Delete event "${eventName}"? This cannot be undone.`)) {
      deleteEvent.mutate(eventId);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Events</h1>
        <Button asChild>
          <Link to={ROUTES.ADMIN_EVENTS_NEW}>
            <Plus className="mr-2 h-4 w-4" />
            Create Event
          </Link>
        </Button>
      </div>

      {isLoading && (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Event</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Date</TableHead>
                <TableHead>Tickets</TableHead>
                <TableHead className="text-right">Revenue</TableHead>
                <TableHead className="w-24">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {Array.from({ length: 8 }, (_, i) => i).map((n) => (
                <TableRow key={`event-skeleton-${n}`}>
                  <TableCell>
                    <Skeleton className="h-4 w-32" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-5 w-16 rounded-full" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-4 w-24" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-4 w-16" />
                  </TableCell>
                  <TableCell className="text-right">
                    <Skeleton className="ml-auto h-4 w-20" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-8 w-16" />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {!isLoading && events.length > 0 && (
        <>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Event</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead>Tickets</TableHead>
                  <TableHead className="text-right">Revenue</TableHead>
                  <TableHead className="w-24">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {events.map((event) => (
                  <TableRow key={event.id}>
                    <TableCell>
                      <div>
                        <p className="font-medium">{event.name}</p>
                        {event.artistName && (
                          <p className="text-xs text-muted-foreground">{event.artistName}</p>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant={statusVariants[event.status] ?? 'secondary'}>
                        {event.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {formatShortDate(event.eventDate)}
                    </TableCell>
                    <TableCell className="text-sm">
                      {event.soldTicketCount} / {event.totalTicketCount}
                    </TableCell>
                    <TableCell className="text-right text-sm">
                      {formatCurrency(event.totalRevenue)}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        <Button variant="ghost" size="icon" asChild>
                          <Link to={`/admin/events/${event.id}/edit`}>
                            <Pencil className="h-4 w-4" />
                            <span className="sr-only">Edit</span>
                          </Link>
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleDelete(event.id, event.name)}
                          disabled={deleteEvent.isPending}
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
                          <span className="sr-only">Delete</span>
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          {/* Pagination */}
          {eventsPage && eventsPage.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={eventsPage.first}
              >
                Previous
              </Button>
              <span className="text-sm text-muted-foreground">
                Page {eventsPage.number + 1} of {eventsPage.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={eventsPage.last}
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}

      {!isLoading && events.length === 0 && (
        <p className="py-8 text-center text-muted-foreground">
          No events found. Create your first event to get started.
        </p>
      )}
    </div>
  );
}
