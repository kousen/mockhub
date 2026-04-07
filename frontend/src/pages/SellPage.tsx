import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { Loader2, Search, DollarSign, Ticket, MapPin, Calendar } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useEvents, useEventSections } from '@/hooks/use-events';
import { useCreateListing, useMyOwnedTickets } from '@/hooks/use-seller';
import { VenueMap } from '@/components/tickets/VenueMap';
import type { EventSummary } from '@/types/event';
import type { OwnedTicket, SellListingRequest } from '@/types/seller';

type Step = 'event' | 'seat' | 'price';

/**
 * Multi-step form for creating a new ticket listing.
 * Shows owned tickets first for quick listing, with fallback to event search.
 * Step 1: Select from owned tickets or search for an event
 * Step 2: Enter seat details (section, row, seat number) with optional VenueMap
 * Step 3: Set the listing price
 */
export function SellPage() {
  const navigate = useNavigate();
  const createListing = useCreateListing();

  const [step, setStep] = useState<Step>('event');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedEvent, setSelectedEvent] = useState<EventSummary | null>(null);
  const [sectionName, setSectionName] = useState('');
  const [rowLabel, setRowLabel] = useState('');
  const [seatNumber, setSeatNumber] = useState('');
  const [listedPrice, setListedPrice] = useState('');

  const { data: ownedTickets, isLoading: ownedTicketsLoading } = useMyOwnedTickets();

  const { data: sections, isLoading: sectionsLoading } = useEventSections(
    selectedEvent?.slug ?? '',
  );

  const { data: eventsPage, isLoading: eventsLoading } = useEvents({
    q: searchQuery.length >= 2 ? searchQuery : undefined,
    size: 10,
  });

  const events = eventsPage?.content ?? [];

  const handleSelectEvent = useCallback((event: EventSummary) => {
    setSelectedEvent(event);
    setSectionName('');
    setRowLabel('');
    setSeatNumber('');
    setStep('seat');
  }, []);

  const handleSelectOwnedTicket = useCallback((ticket: OwnedTicket) => {
    const eventSummary: EventSummary = {
      id: 0,
      name: ticket.eventName,
      slug: ticket.eventSlug,
      artistName: null,
      venueName: ticket.venueName,
      city: '',
      eventDate: ticket.eventDate,
      categoryName: '',
      primaryImageUrl: null,
      minPrice: null,
      availableTickets: 0,
      isFeatured: false,
    };
    setSelectedEvent(eventSummary);
    setSectionName(ticket.sectionName);
    setRowLabel(ticket.rowLabel ?? '');
    setSeatNumber(ticket.seatNumber ?? '');

    // Skip to price only if we have complete seat info; otherwise go to seat step
    if (ticket.rowLabel && ticket.seatNumber) {
      setStep('price');
    } else {
      setStep('seat');
    }
  }, []);

  const handleSeatSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      if (!sectionName.trim()) {
        toast.error('Section name is required.');
        return;
      }
      if (!rowLabel.trim()) {
        toast.error('Row label is required.');
        return;
      }
      if (!seatNumber.trim()) {
        toast.error('Seat number is required.');
        return;
      }
      setStep('price');
    },
    [sectionName, rowLabel, seatNumber],
  );

  const handleSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      if (!selectedEvent) return;

      const price = Number.parseFloat(listedPrice);
      if (Number.isNaN(price) || price <= 0) {
        toast.error('Please enter a valid price greater than $0.');
        return;
      }

      const request: SellListingRequest = {
        eventSlug: selectedEvent.slug,
        sectionName: sectionName.trim(),
        rowLabel: rowLabel.trim(),
        seatNumber: seatNumber.trim(),
        price,
      };

      createListing.mutate(request, {
        onSuccess: () => {
          toast.success('Listing created successfully!');
          navigate('/my/listings');
        },
        onError: (error: unknown) => {
          const apiError = error as { response?: { data?: { detail?: string } } };
          const detail = apiError?.response?.data?.detail;
          toast.error(detail || 'Failed to create listing. Please try again.');
        },
      });
    },
    [selectedEvent, sectionName, rowLabel, seatNumber, listedPrice, createListing, navigate],
  );

  const handleSectionSelect = useCallback(
    (sectionId: number | null) => {
      if (sectionId === null || !sections) {
        setSectionName('');
        return;
      }
      const section = sections.find((s) => s.sectionId === sectionId);
      if (section) {
        setSectionName(section.sectionName);
      }
    },
    [sections],
  );

  const renderSectionInput = () => {
    if (sectionsLoading) {
      return (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="h-4 w-4 animate-spin" />
          Loading sections...
        </div>
      );
    }
    if (sections && sections.length > 0) {
      return (
        <Select value={sectionName} onValueChange={setSectionName}>
          <SelectTrigger id="section">
            <SelectValue placeholder="Select a section" />
          </SelectTrigger>
          <SelectContent>
            {sections.map((section) => (
              <SelectItem key={section.sectionId} value={section.sectionName}>
                {section.sectionName} ({section.availableTickets} available)
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      );
    }
    return (
      <Input
        id="section"
        placeholder="e.g., Floor, Section 101, GA"
        value={sectionName}
        onChange={(e) => setSectionName(e.target.value)}
        required
      />
    );
  };

  const selectedSectionId = sections?.find((s) => s.sectionName === sectionName)?.sectionId ?? null;

  const hasSvgSections = sections?.some((s) => s.svgX !== null) ?? false;

  return (
    <div className="mx-auto max-w-2xl px-4 py-6 sm:px-6 lg:px-8">
      <h1 className="mb-6 text-2xl font-bold">Sell Tickets</h1>

      {/* Step Indicator */}
      <div className="mb-8 flex items-center gap-2">
        {(['event', 'seat', 'price'] as const).map((s, index) => (
          <div key={s} className="flex items-center gap-2">
            {index > 0 && (
              <div
                className={`h-px w-8 ${step === s || ['seat', 'price'].indexOf(step) > ['seat', 'price'].indexOf(s) ? 'bg-primary' : 'bg-border'}`}
              />
            )}
            <div
              className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium ${(() => {
                const steps = ['event', 'seat', 'price'] as const;
                if (step === s) return 'bg-primary text-primary-foreground';
                if (steps.indexOf(s) < steps.indexOf(step)) return 'bg-primary/20 text-primary';
                return 'bg-muted text-muted-foreground';
              })()}`}
            >
              {index + 1}
            </div>
            <span
              className={`hidden text-sm sm:inline ${step === s ? 'font-medium' : 'text-muted-foreground'}`}
            >
              {(() => {
                if (s === 'event') return 'Event';
                if (s === 'seat') return 'Seat Details';
                return 'Set Price';
              })()}
            </span>
          </div>
        ))}
      </div>

      {/* Step 1: Select Event */}
      {step === 'event' && (
        <div className="space-y-6">
          {/* Owned Tickets Section */}
          {ownedTicketsLoading && (
            <Card>
              <CardContent className="flex items-center justify-center py-8">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
              </CardContent>
            </Card>
          )}

          {ownedTickets && ownedTickets.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Your Tickets</CardTitle>
                <CardDescription>Select a ticket you own to list it for sale.</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  {ownedTickets.map((ticket) => (
                    <button
                      key={ticket.ticketId}
                      onClick={() => handleSelectOwnedTicket(ticket)}
                      className="w-full rounded-lg border p-4 text-left transition-colors hover:bg-accent"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0 flex-1">
                          <p className="font-medium">{ticket.eventName}</p>
                          <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
                            <span className="flex items-center gap-1">
                              <Calendar className="h-3.5 w-3.5" />
                              {new Date(ticket.eventDate).toLocaleDateString()}
                            </span>
                            <span className="flex items-center gap-1">
                              <MapPin className="h-3.5 w-3.5" />
                              {ticket.venueName}
                            </span>
                            <span className="flex items-center gap-1">
                              <Ticket className="h-3.5 w-3.5" />
                              {ticket.sectionName}
                              {ticket.rowLabel && `, Row ${ticket.rowLabel}`}
                              {ticket.seatNumber && `, Seat ${ticket.seatNumber}`}
                            </span>
                          </div>
                        </div>
                        <span className="shrink-0 text-sm text-muted-foreground">
                          ${ticket.faceValue.toFixed(2)}
                        </span>
                      </div>
                    </button>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Event Search */}
          <Card>
            <CardHeader>
              <CardTitle>
                {ownedTickets && ownedTickets.length > 0
                  ? 'Or Search for a Different Event'
                  : 'Select an Event'}
              </CardTitle>
              <CardDescription>Search for the event you have tickets for.</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="relative mb-4">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  placeholder="Search events..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-9"
                />
              </div>

              {eventsLoading && (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                </div>
              )}

              {!eventsLoading && searchQuery.length >= 2 && events.length === 0 && (
                <p className="py-8 text-center text-sm text-muted-foreground">
                  No events found. Try a different search term.
                </p>
              )}

              {!eventsLoading && searchQuery.length < 2 && (
                <p className="py-8 text-center text-sm text-muted-foreground">
                  Type at least 2 characters to search for events.
                </p>
              )}

              <div className="space-y-2">
                {events.map((event) => (
                  <button
                    key={event.id}
                    onClick={() => handleSelectEvent(event)}
                    className="w-full rounded-lg border p-4 text-left transition-colors hover:bg-accent"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <p className="font-medium">{event.name}</p>
                        <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
                          <span className="flex items-center gap-1">
                            <Calendar className="h-3.5 w-3.5" />
                            {new Date(event.eventDate).toLocaleDateString()}
                          </span>
                          <span className="flex items-center gap-1">
                            <MapPin className="h-3.5 w-3.5" />
                            {event.venueName}, {event.city}
                          </span>
                        </div>
                      </div>
                      {event.minPrice !== null && (
                        <span className="shrink-0 text-sm text-muted-foreground">
                          from ${event.minPrice.toFixed(2)}
                        </span>
                      )}
                    </div>
                  </button>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Step 2: Enter Seat Details */}
      {step === 'seat' && selectedEvent && (
        <Card>
          <CardHeader>
            <CardTitle>Seat Details</CardTitle>
            <CardDescription>
              Enter the seat details for{' '}
              <span className="font-medium text-foreground">{selectedEvent.name}</span>
            </CardDescription>
          </CardHeader>
          <CardContent>
            {/* VenueMap for visual section selection */}
            {hasSvgSections && sections && (
              <div className="mb-6">
                <Label className="mb-2 block">Select a section on the map</Label>
                <VenueMap
                  sections={sections}
                  selectedSectionId={selectedSectionId}
                  onSectionSelect={handleSectionSelect}
                />
              </div>
            )}

            <form onSubmit={handleSeatSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="section">Section</Label>
                {renderSectionInput()}
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="row">Row</Label>
                  <Input
                    id="row"
                    placeholder="e.g., A, 12"
                    value={rowLabel}
                    onChange={(e) => setRowLabel(e.target.value)}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="seat">Seat Number</Label>
                  <Input
                    id="seat"
                    placeholder="e.g., 5, 23"
                    value={seatNumber}
                    onChange={(e) => setSeatNumber(e.target.value)}
                    required
                  />
                </div>
              </div>
              <div className="flex gap-3 pt-2">
                <Button type="button" variant="outline" onClick={() => setStep('event')}>
                  Back
                </Button>
                <Button type="submit" className="flex-1">
                  Continue
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Step 3: Set Price */}
      {step === 'price' && selectedEvent && (
        <Card>
          <CardHeader>
            <CardTitle>Set Your Price</CardTitle>
            <CardDescription>
              Choose a price for your ticket to{' '}
              <span className="font-medium text-foreground">{selectedEvent.name}</span>
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Listing Summary */}
              <div className="rounded-lg bg-muted/50 p-4">
                <div className="flex items-center gap-2 text-sm">
                  <Ticket className="h-4 w-4 text-muted-foreground" />
                  <span className="font-medium">{selectedEvent.name}</span>
                </div>
                <p className="mt-1 text-sm text-muted-foreground">
                  {sectionName} &middot; Row {rowLabel} &middot; Seat {seatNumber}
                </p>
                {selectedEvent.minPrice !== null && (
                  <p className="mt-2 text-xs text-muted-foreground">
                    Current lowest price: ${selectedEvent.minPrice.toFixed(2)}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="price">Listing Price ($)</Label>
                <div className="relative">
                  <DollarSign className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="price"
                    type="number"
                    min="0.01"
                    step="0.01"
                    placeholder="0.00"
                    value={listedPrice}
                    onChange={(e) => setListedPrice(e.target.value)}
                    className="pl-9"
                    required
                  />
                </div>
              </div>

              <div className="flex gap-3 pt-2">
                <Button type="button" variant="outline" onClick={() => setStep('seat')}>
                  Back
                </Button>
                <Button type="submit" className="flex-1" disabled={createListing.isPending}>
                  {createListing.isPending ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Creating Listing...
                    </>
                  ) : (
                    'Create Listing'
                  )}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
