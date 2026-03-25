import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { SellPage } from './SellPage';
import type { EventSummary } from '@/types/event';
import type { PageResponse } from '@/types/common';

vi.mock('@/hooks/use-events', () => ({
  useEvents: vi.fn(() => ({
    data: undefined,
    isLoading: false,
  })),
}));

vi.mock('@/hooks/use-seller', () => ({
  useCreateListing: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
}));

import { useEvents } from '@/hooks/use-events';
import { useCreateListing } from '@/hooks/use-seller';

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
  } as ReturnType<typeof useEvents>);
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

  it('shows step 3 after filling seat details', async () => {
    setEvents(mockEventsPage);

    const user = userEvent.setup();
    renderWithProviders(<SellPage />);

    // Select event
    await user.click(screen.getByText('Taylor Swift - Eras Tour'));

    // Fill seat details
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
});
