import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { VenueMap } from './VenueMap';
import type { SectionAvailability } from '@/types/ticket';

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = { isAuthenticated: false, user: null, accessToken: null };
    return selector(state);
  }),
}));

vi.mock('@/stores/cart-store', () => ({
  useCartStore: vi.fn((selector) => {
    const state = { itemCount: 0, openDrawer: vi.fn() };
    return selector(state);
  }),
}));

const mockSections: SectionAvailability[] = [
  {
    sectionId: 1,
    sectionName: 'Floor',
    sectionType: 'FLOOR',
    totalTickets: 100,
    availableTickets: 45,
    minPrice: 75,
    maxPrice: 150,
    colorHex: '#FF4444',
    svgPathId: 'floor',
    svgX: 50,
    svgY: 45,
    svgWidth: 500,
    svgHeight: 80,
  },
  {
    sectionId: 2,
    sectionName: 'Lower Bowl',
    sectionType: 'LOWER',
    totalTickets: 200,
    availableTickets: 120,
    minPrice: 50,
    maxPrice: 100,
    colorHex: '#FF8800',
    svgPathId: 'lower-bowl',
    svgX: 50,
    svgY: 133,
    svgWidth: 500,
    svgHeight: 80,
  },
  {
    sectionId: 3,
    sectionName: 'Upper Deck',
    sectionType: 'UPPER',
    totalTickets: 150,
    availableTickets: 0,
    minPrice: null,
    maxPrice: null,
    colorHex: '#44BB44',
    svgPathId: 'upper-deck',
    svgX: 50,
    svgY: 221,
    svgWidth: 500,
    svgHeight: 80,
  },
];

describe('VenueMap', () => {
  it('renders SVG with section rectangles', () => {
    renderWithProviders(
      <VenueMap sections={mockSections} selectedSectionId={null} onSectionSelect={vi.fn()} />,
    );

    const svg = screen.getByLabelText('Venue seating map');
    expect(svg).toBeDefined();

    const sectionButtons = screen.getAllByRole('button');
    expect(sectionButtons).toHaveLength(3);
  });

  it('clicking available section calls onSectionSelect', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();

    renderWithProviders(
      <VenueMap sections={mockSections} selectedSectionId={null} onSectionSelect={onSelect} />,
    );

    const floorSection = screen.getByRole('button', { name: /Floor section/ });
    await user.click(floorSection);

    expect(onSelect).toHaveBeenCalledWith(1);
  });

  it('clicking selected section deselects it', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();

    renderWithProviders(
      <VenueMap sections={mockSections} selectedSectionId={1} onSectionSelect={onSelect} />,
    );

    const floorSection = screen.getByRole('button', { name: /Floor section/ });
    await user.click(floorSection);

    expect(onSelect).toHaveBeenCalledWith(null);
  });

  it('unavailable sections are not clickable', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();

    renderWithProviders(
      <VenueMap sections={mockSections} selectedSectionId={null} onSectionSelect={onSelect} />,
    );

    const upperDeck = screen.getByRole('button', { name: /Upper Deck section/ });
    await user.click(upperDeck);

    expect(onSelect).not.toHaveBeenCalled();
  });

  it('falls back to card grid when no SVG data', () => {
    const sectionsWithoutSvg: SectionAvailability[] = mockSections.map((section) => ({
      ...section,
      svgPathId: null,
      svgX: null,
      svgY: null,
      svgWidth: null,
      svgHeight: null,
    }));

    renderWithProviders(
      <VenueMap sections={sectionsWithoutSvg} selectedSectionId={null} onSectionSelect={vi.fn()} />,
    );

    // SeatSelector renders cards, not an SVG
    expect(screen.queryByRole('img', { name: 'Venue seating map' })).toBeNull();
    // Should show section names as card content
    expect(screen.getByText('Floor')).toBeDefined();
    expect(screen.getByText('Lower Bowl')).toBeDefined();
  });

  it('keyboard Enter selects section', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();

    renderWithProviders(
      <VenueMap sections={mockSections} selectedSectionId={null} onSectionSelect={onSelect} />,
    );

    const floorSection = screen.getByRole('button', { name: /Floor section/ });
    floorSection.focus();
    await user.keyboard('{Enter}');

    expect(onSelect).toHaveBeenCalledWith(1);
  });
});
