import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router';
import { Loader2, MapPin, Calendar, Ticket, XCircle } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { getPublicOrderView } from '@/api/public-tickets';
import type { PublicOrderView } from '@/types/public-ticket';

/**
 * Public ticket view page. Accessed via SMS link with a signed token.
 * Shows scannable QR codes for each ticket — no login required.
 */
export function PublicTicketViewPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [order, setOrder] = useState<PublicOrderView | null>(null);
  const [loading, setLoading] = useState(!!token);
  const [error, setError] = useState<string | null>(token ? null : 'No ticket token provided');

  useEffect(() => {
    if (!token) return;

    getPublicOrderView(token)
      .then(setOrder)
      .catch(() => {
        setError('Unable to load tickets. The link may be invalid or expired.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [token]);

  if (loading) {
    return (
      <div className="flex min-h-[calc(100vh-10rem)] items-center justify-center">
        <div className="text-center">
          <Loader2 className="mx-auto h-12 w-12 animate-spin text-muted-foreground" />
          <p className="mt-4 text-muted-foreground">Loading your tickets...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-[calc(100vh-10rem)] items-center justify-center">
        <div className="text-center">
          <XCircle className="mx-auto h-16 w-16 text-red-500" />
          <h1 className="mt-4 text-2xl font-bold">Couldn&apos;t Load Tickets</h1>
          <p className="mt-2 text-muted-foreground">{error}</p>
        </div>
      </div>
    );
  }

  if (!order) return null;

  return (
    <div className="mx-auto max-w-md px-4 py-6 sm:px-6">
      {/* Event Header */}
      <div className="text-center">
        <h1 className="text-2xl font-bold">{order.eventName}</h1>
        <div className="mt-2 flex flex-col items-center gap-1 text-sm text-muted-foreground">
          <span className="flex items-center gap-1">
            <Calendar className="h-4 w-4" />
            {order.eventDate}
          </span>
          <span className="flex items-center gap-1">
            <MapPin className="h-4 w-4" />
            {order.venueName} &middot; {order.venueLocation}
          </span>
        </div>
        <p className="mt-1 text-xs text-muted-foreground">Order {order.orderNumber}</p>
      </div>

      {/* Tickets */}
      <div className="mt-6 space-y-6">
        {order.tickets.map((ticket) => (
          <div key={ticket.ticketId} className="rounded-xl border bg-card p-4 shadow-sm">
            {/* Seat Info */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Ticket className="h-4 w-4 text-primary" />
                <span className="font-semibold">{ticket.sectionName}</span>
              </div>
              <Badge variant="secondary">{ticket.ticketType}</Badge>
            </div>
            {(ticket.rowLabel || ticket.seatNumber) && (
              <p className="mt-1 text-sm text-muted-foreground">
                {ticket.rowLabel && `Row ${ticket.rowLabel}`}
                {ticket.rowLabel && ticket.seatNumber && ' · '}
                {ticket.seatNumber && `Seat ${ticket.seatNumber}`}
              </p>
            )}

            {/* QR Code */}
            <div className="mt-4 flex justify-center">
              <img
                src={ticket.qrCodeUrl}
                alt={`QR code for ticket ${ticket.ticketId}`}
                className="h-64 w-64"
              />
            </div>
          </div>
        ))}
      </div>

      {/* Footer */}
      <p className="mt-6 text-center text-sm text-muted-foreground">
        Show this QR code at the venue entrance.
      </p>
    </div>
  );
}
