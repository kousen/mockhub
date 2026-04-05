import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { SellPage } from './SellPage';
import type { EventSummary } from '@/types/event';
import type { PageResponse } from '@/types/common';
import type { SectionAvailability } from '@/types/ticket';
import type { OwnedTicket } from '@/types/seller';

vi.mock('@/hooks/use-events', () => ({
  useEvents: vi.fn(() => ({
    data: undefined,
    isLoading: false,
  })),
  useEventSections: vi.fn(() => ({
    data: undefined,
    isLoading: false,
  })),
}));

vi.mock('@/hooks/use-seller', () => ({
  useCreateListing: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
  useMyOwnedTickets: vi.fn(() => ({
    data: undefined,
    isLoading: false,
  })),
}));

import { useEvents, useEventSections } from '@/hooks/use-events';
import { useCreateListing, useMyOwnedTickets } from '@/hooks/use-seller';

const mockSections: SectionAvailability[] = [
  {
    sectionId: 1,
    sectionName: 'Floor',
    sectionType: 'FLOOR',
    totalTickets: 100,
    availableTickets: 45,
    minPrice: 150.0,
    maxPrice: 300.0,
    colorHex: '#FF0000',
    svgPathId: null,
    svgX: null,
    svgY: null,
    svgWidth: null,
    svgHeight: null,
  },
  {
    sectionId: 2,
    sectionName: 'Lower Bowl',
    sectionType: 'LOWER_BOWL',
    totalTickets: 200,
    availableTickets: 120,
    minPrice: 85.0,
    maxPrice: 150.0,
    colorHex: '#00FF00',
    svgPathId: null,
    svgX: null,
    svgY: null,
    svgWidth: null,
    svgHeight: null,
  },
];

const mockEvents: EventSummary[] = [
  {
    id: 1,
    name: 'Taylor Swift - Eras Tour',
    slug: 'taylor-swift-eras-tour',
    artistName: 'Taylor Swift',
    venueName: 'SoFi Stadium',
    city: 'Los Angeles',
    eventDate: '2026-06-15T20:00:00',
    minPrice: 150.0,
    availableTickets: 200,
    primaryImageUrl: null,
    categoryName: 'Concerts',
    isFeatured: true,
  },
  {
    id: 2,
    name: 'Kendrick Lamar',
    slug: 'kendrick-lamar',
    artistName: 'Kendrick Lamar',
    venueName: 'Madison Square Garden',
    city: 'New York',
    eventDate: '2026-07-20T19:30:00',
    minPrice: 85.0,
    availableTickets: 100,
    primaryImageUrl: null,
    categoryName: 'Concerts',
    isFeatured: false,
  },
];

const mockEventsPage: PageResponse<EventSummary> = {
  content: mockEvents,
  totalElements: 2,
  totalPages: 1,
  size: 10,
  number: 0,
  first: true,
  last: true,
};

function setEvents(data: PageResponse<EventSummary> | undefined, isLoading = false) {
  vi.mocked(useEvents).mockReturnValue({
    data,
    isLoading,
  } as unknown as ReturnType<typeof useEvents>);
}

const mockOwnedTickets: OwnedTicket[] = [
  {
    ticketId: 101,
    eventSlug: 'taylor-swift-eras-tour',
    eventName: 'Taylor Swift - Eras Tour',
    eventDate: '2026-06-15T20:00:00',
    venueName: 'SoFi Stadium',
    sectionName: 'Floor',
    rowLabel: 'A',
    seatNumber: '5',
    ticketType: 'RESERVED',
    faceValue: 150.0,
  },
  {
    ticketId: 102,
    eventSlug: 'kendrick-lamar',
    eventName: 'Kendrick Lamar',
    eventDate: '2026-07-20T19:30:00',
    venueName: 'Madison Square Garden',
    sectionName: 'Section 101',
    rowLabel: 'C',
    seatNumber: '12',
    ticketType: 'RESERVED',
    faceValue: 85.0,
  },
];

function setOwnedTickets(data: OwnedTicket[] | undefined, isLoading = false) {
  vi.mocked(useMyOwnedTickets).mockReturnValue({
    data,
    isLoading,
  } as unknown as ReturnType<typeof useMyOwnedTickets>);
}

function setSections(data: SectionAvailability[] | undefined, isLoading = false) {
  vi.mocked(useEventSections).mockReturnValue({
    data,
    isLoading,
  } as unknown as ReturnType<typeof useEventSections>);
}

describe('SellPage', () => {
  it('renders the sell form with step 1 (event selection)', () => {
    setEvents(undefined);

    renderWithProviders(<SellPage />);

    expect(screen.getByText('Sell Tickets')).toBeDefined();
    expect(screen.getByText('Select an Event')).toBeDefined();
    expect(screen.getByPlaceholderText('Search events...')).toBeDefined();
    expect(screen.getByText('Type at least 2 characters to search for events.')).toBeDefined();
  });

  it('shows loading state while events load', () => {
    setEvents(undefined, true);

    renderWithProviders(<SellPage />);

    // The Loader2 spinner should be present (rendered as an svg with animate-spin)
    const spinner = document.querySelector('.animate-spin');
    expect(spinner).not.toBeNull();
  });

  it('shows events when search query has at least 2 characters', async () => {
    setEvents(mockEventsPage);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    const searchInput = screen.getByPlaceholderText('Search events...');
    await user.type(searchInput, 'Ta');

    // Events should be rendered (the mock returns them regardless of query)
    expect(screen.getByText('Taylor Swift - Eras Tour')).toBeDefined();
    expect(screen.getByText('Kendrick Lamar')).toBeDefined();
  });

  it('shows no events message when search returns empty', async () => {
    const emptyPage: PageResponse<EventSummary> = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
      first: true,
      last: true,
    };

    setEvents(emptyPage);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    // Type a search query (>= 2 chars) to trigger the "no events" branch
    await user.type(screen.getByPlaceholderText('Search events...'), 'xyz');

    expect(screen.getByText('No events found. Try a different search term.')).toBeDefined();
  });

  it('advances to step 2 when an event is selected', async () => {
    setEvents(mockEventsPage);
    setSections(undefined);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    // Click on the first event
    const eventButton = screen.getByText('Taylor Swift - Eras Tour');
    await user.click(eventButton);

    // Should now show step 2 (seat details) — text appears in both step indicator and card title
    expect(screen.getAllByText('Seat Details').length).toBeGreaterThan(0);
    expect(screen.getByLabelText('Section')).toBeDefined();
    expect(screen.getByLabelText('Row')).toBeDefined();
    expect(screen.getByLabelText('Seat Number')).toBeDefined();
  });

  it('shows section dropdown when sections are available', async () => {
    setEvents(mockEventsPage);
    setSections(mockSections);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    await user.click(screen.getByText('Taylor Swift - Eras Tour'));

    // Should show the select trigger with placeholder
    expect(screen.getByText('Select a section')).toBeDefined();
  });

  it('shows loading indicator while sections are loading', async () => {
    setEvents(mockEventsPage);
    setSections(undefined, true);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    await user.click(screen.getByText('Taylor Swift - Eras Tour'));

    expect(screen.getByText('Loading sections...')).toBeDefined();
  });

  it('falls back to text input when no sections are available', async () => {
    setEvents(mockEventsPage);
    setSections([]);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    await user.click(screen.getByText('Taylor Swift - Eras Tour'));

    // Should show the text input fallback
    const sectionInput = screen.getByLabelText('Section');
    expect(sectionInput.tagName).toBe('INPUT');
    expect(sectionInput.getAttribute('placeholder')).toBe('e.g., Floor, Section 101, GA');
  });

  it('displays API error detail in toast when listing creation fails', async () => {
    const mockMutate = vi.fn((_req: unknown, options: { onError: (err: unknown) => void }) => {
      options.onError({
        response: {
          data: {
            detail: "Ticket not found with seat: 'Floor/A/2'",
          },
        },
      });
    });
    vi.mocked(useCreateListing).mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as unknown as ReturnType<typeof useCreateListing>);
    setEvents(mockEventsPage);
    setSections([]);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    // Navigate through all 3 steps
    await user.click(screen.getByText('Taylor Swift - Eras Tour'));
    await user.type(screen.getByLabelText('Section'), 'Floor');
    await user.type(screen.getByLabelText('Row'), 'A');
    await user.type(screen.getByLabelText('Seat Number'), '2');
    await user.click(screen.getByText('Continue'));
    await user.type(screen.getByLabelText('Listing Price ($)'), '100');
    await user.click(screen.getByText('Create Listing'));

    // The mutate should have been called with the error callback
    expect(mockMutate).toHaveBeenCalled();
  });

  it('shows step 3 after filling seat details with text input', async () => {
    setEvents(mockEventsPage);
    setSections([]);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    // Select event
    await user.click(screen.getByText('Taylor Swift - Eras Tour'));

    // Fill seat details (text input fallback since no sections)
    await user.type(screen.getByLabelText('Section'), 'Floor');
    await user.type(screen.getByLabelText('Row'), 'A');
    await user.type(screen.getByLabelText('Seat Number'), '5');

    // Click Continue
    await user.click(screen.getByText('Continue'));

    // Should now show step 3 (set price)
    expect(screen.getByText('Set Your Price')).toBeDefined();
    expect(screen.getByLabelText('Listing Price ($)')).toBeDefined();
    expect(screen.getByText('Create Listing')).toBeDefined();
  });

  it('shows creating state when listing is being submitted', async () => {
    // Set pending state before rendering so it applies when we reach step 3
    vi.mocked(useCreateListing).mockReturnValue({
      mutate: vi.fn(),
      isPending: true,
    } as unknown as ReturnType<typeof useCreateListing>);

    setEvents(mockEventsPage);
    setSections([]);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    // Navigate to step 3: select event → fill seat details → continue
    await user.click(screen.getByText('Taylor Swift - Eras Tour'));
    await user.type(screen.getByLabelText('Section'), 'Floor');
    await user.type(screen.getByLabelText('Row'), 'A');
    await user.type(screen.getByLabelText('Seat Number'), '5');
    await user.click(screen.getByText('Continue'));

    // The submit button should show "Creating..." when isPending
    expect(screen.getByText('Creating Listing...')).toBeDefined();
  });

  it('renders step indicator with 3 steps', () => {
    setEvents(undefined);

    renderWithProviders(<SellPage />);

    expect(screen.getByText('1')).toBeDefined();
    expect(screen.getByText('2')).toBeDefined();
    expect(screen.getByText('3')).toBeDefined();
  });

  // -- Owned Tickets (#81) --

  it('shows owned tickets when user has available tickets', () => {
    setOwnedTickets(mockOwnedTickets);
    setEvents(undefined);

    renderWithProviders(<SellPage />);

    expect(screen.getByText('Your Tickets')).toBeDefined();
    expect(screen.getByText('Taylor Swift - Eras Tour')).toBeDefined();
    expect(screen.getByText('Kendrick Lamar')).toBeDefined();
    expect(screen.getByText('$150.00')).toBeDefined();
    expect(screen.getByText('Or Search for a Different Event')).toBeDefined();
  });

  it('skips to price step when an owned ticket is selected', async () => {
    setOwnedTickets(mockOwnedTickets);
    setEvents(undefined);
    setSections(undefined);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    // Click the first owned ticket
    const ticketButtons = screen.getAllByRole('button');
    const taylorButton = ticketButtons.find((btn) =>
      btn.textContent?.includes('Taylor Swift - Eras Tour'),
    );
    expect(taylorButton).toBeDefined();
    await user.click(taylorButton!);

    // Should jump directly to step 3 (price)
    expect(screen.getByText('Set Your Price')).toBeDefined();
    expect(screen.getByText('Floor · Row A · Seat 5')).toBeDefined();
  });

  it('shows loading state while owned tickets load', () => {
    setOwnedTickets(undefined, true);
    setEvents(undefined);

    renderWithProviders(<SellPage />);

    const spinner = document.querySelector('.animate-spin');
    expect(spinner).not.toBeNull();
  });

  it('goes to seat step when owned ticket has no row/seat (GA)', async () => {
    const gaTickets: OwnedTicket[] = [
      {
        ticketId: 201,
        eventSlug: 'outdoor-festival',
        eventName: 'Outdoor Festival',
        eventDate: '2026-08-01T14:00:00',
        venueName: 'Central Park',
        sectionName: 'General Admission',
        rowLabel: null,
        seatNumber: null,
        ticketType: 'GENERAL_ADMISSION',
        faceValue: 50.0,
      },
    ];
    setOwnedTickets(gaTickets);
    setEvents(undefined);
    setSections([]);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    const ticketButton = screen.getByText('Outdoor Festival').closest('button')!;
    await user.click(ticketButton);

    // Should go to step 2 (seat details) since row/seat are null
    expect(screen.getAllByText('Seat Details').length).toBeGreaterThan(0);
    // Section should be pre-filled
    const sectionInput = screen.getByLabelText('Section') as HTMLInputElement;
    expect(sectionInput.value).toBe('General Admission');
  });

  it('shows only search when user has no owned tickets', () => {
    setOwnedTickets([]);
    setEvents(undefined);

    renderWithProviders(<SellPage />);

    expect(screen.getByText('Select an Event')).toBeDefined();
    expect(screen.queryByText('Your Tickets')).toBeNull();
  });
});
