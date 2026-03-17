import { describe, it, expect } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { EventCard } from './EventCard';
import type { EventSummary } from '@/types/event';

const mockEvent: EventSummary = {
  id: 1,
  name: 'Rock Festival 2026',
  slug: 'rock-festival-2026',
  artistName: 'The Rockers',
  venueName: 'Madison Square Garden',
  city: 'New York',
  eventDate: '2026-06-15T20:00:00Z',
  minPrice: 75.0,
  availableTickets: 500,
  primaryImageUrl: null,
  categoryName: 'Concert',
  isFeatured: true,
};

describe('EventCard', () => {
  it('renders event name', () => {
    renderWithProviders(<EventCard event={mockEvent} />);
    const elements = screen.getAllByText('Rock Festival 2026');
    expect(elements.length).toBeGreaterThan(0);
    // Verify the card title specifically
    const cardTitle = elements.find((el) => el.getAttribute('data-slot') === 'card-title');
    expect(cardTitle).toBeDefined();
  });

  it('renders artist name when provided', () => {
    renderWithProviders(<EventCard event={mockEvent} />);
    expect(screen.getByText('The Rockers')).toBeDefined();
  });

  it('renders venue and city', () => {
    renderWithProviders(<EventCard event={mockEvent} />);
    expect(screen.getByText('Madison Square Garden, New York')).toBeDefined();
  });

  it('renders category badge', () => {
    renderWithProviders(<EventCard event={mockEvent} />);
    expect(screen.getByText('Concert')).toBeDefined();
  });

  it('renders available tickets count', () => {
    renderWithProviders(<EventCard event={mockEvent} />);
    expect(screen.getByText('500 tickets available')).toBeDefined();
  });

  it('renders "View Tickets" link', () => {
    renderWithProviders(<EventCard event={mockEvent} />);
    expect(screen.getByText('View Tickets')).toBeDefined();
  });

  it('links to event detail page', () => {
    renderWithProviders(<EventCard event={mockEvent} />);
    const links = screen.getAllByRole('link');
    const eventLinks = links.filter(
      (link) => link.getAttribute('href') === '/events/rock-festival-2026',
    );
    expect(eventLinks.length).toBeGreaterThan(0);
  });

  it('renders singular "ticket" for single available ticket', () => {
    const singleTicketEvent = { ...mockEvent, availableTickets: 1 };
    renderWithProviders(<EventCard event={singleTicketEvent} />);
    expect(screen.getByText('1 ticket available')).toBeDefined();
  });

  it('does not render artist name when null', () => {
    const noArtistEvent = { ...mockEvent, artistName: null };
    renderWithProviders(<EventCard event={noArtistEvent} />);
    expect(screen.queryByText('The Rockers')).toBeNull();
  });
});
