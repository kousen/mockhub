import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { EventGrid } from './EventGrid';
import { renderWithProviders } from '@/test/test-utils';
import type { EventSummary } from '@/types/event';

const mockEvent: EventSummary = {
  id: 1,
  name: 'Rock Festival',
  slug: 'rock-festival',
  artistName: 'The Rockers',
  venueName: 'Madison Square Garden',
  city: 'New York',
  eventDate: new Date(Date.now() + 86400000).toISOString(),
  minPrice: 75,
  availableTickets: 500,
  primaryImageUrl: null,
  categoryName: 'Concert',
  isFeatured: true,
};

describe('EventGrid', () => {
  it('renders loading skeletons when isLoading is true', () => {
    renderWithProviders(<EventGrid events={[]} isLoading={true} />);
    const skeletons = document.querySelectorAll('.animate-pulse');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('renders empty state when events array is empty', () => {
    renderWithProviders(<EventGrid events={[]} />);
    expect(screen.getByText('No events found')).toBeDefined();
    expect(screen.getByText('Try adjusting your search or filter criteria.')).toBeDefined();
  });

  it('renders event cards when events are provided', () => {
    renderWithProviders(<EventGrid events={[mockEvent]} />);
    expect(screen.getAllByText('Rock Festival').length).toBeGreaterThan(0);
  });

  it('renders multiple event cards', () => {
    const events = [mockEvent, { ...mockEvent, id: 2, name: 'Jazz Night', slug: 'jazz-night' }];
    renderWithProviders(<EventGrid events={events} />);
    expect(screen.getAllByText('Rock Festival').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Jazz Night').length).toBeGreaterThan(0);
  });
});
