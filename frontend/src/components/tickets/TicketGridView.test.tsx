import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TicketGridView } from './TicketGridView';
import type { SectionAvailability } from '@/types/ticket';

const mockSections: SectionAvailability[] = [
  {
    sectionId: 1,
    sectionName: 'Floor A',
    sectionType: 'FLOOR',
    totalTickets: 100,
    availableTickets: 42,
    minPrice: 150.0,
    maxPrice: 250.0,
    colorHex: '#FF0000',
    svgPathId: null,
    svgX: null,
    svgY: null,
    svgWidth: null,
    svgHeight: null,
  },
  {
    sectionId: 2,
    sectionName: 'Balcony B',
    sectionType: 'BALCONY',
    totalTickets: 200,
    availableTickets: 0,
    minPrice: 50.0,
    maxPrice: 50.0,
    colorHex: null,
    svgPathId: null,
    svgX: null,
    svgY: null,
    svgWidth: null,
    svgHeight: null,
  },
  {
    sectionId: 3,
    sectionName: 'Floor B',
    sectionType: 'FLOOR',
    totalTickets: 100,
    availableTickets: 10,
    minPrice: null,
    maxPrice: null,
    colorHex: null,
    svgPathId: null,
    svgX: null,
    svgY: null,
    svgWidth: null,
    svgHeight: null,
  },
];

describe('TicketGridView', () => {
  it('renders loading skeletons when isLoading is true', () => {
    const { container } = render(<TicketGridView sections={[]} isLoading={true} />);
    const skeletons = container.querySelectorAll('[data-slot="skeleton"]');
    expect(skeletons.length).toBe(6);
  });

  it('renders empty state when sections array is empty', () => {
    render(<TicketGridView sections={[]} />);
    expect(screen.getByText('No section info available')).toBeDefined();
  });

  it('renders section names grouped by type', () => {
    render(<TicketGridView sections={mockSections} />);
    expect(screen.getByText('Floor A')).toBeDefined();
    expect(screen.getByText('Floor B')).toBeDefined();
    expect(screen.getByText('Balcony B')).toBeDefined();
    // Group headings
    expect(screen.getByText('FLOOR')).toBeDefined();
    expect(screen.getByText('BALCONY')).toBeDefined();
  });

  it('displays availability badges', () => {
    render(<TicketGridView sections={mockSections} />);
    expect(screen.getByText('42 available')).toBeDefined();
    expect(screen.getByText('0 available')).toBeDefined();
    expect(screen.getByText('10 available')).toBeDefined();
  });

  it('displays price range when min and max differ', () => {
    render(<TicketGridView sections={mockSections} />);
    expect(screen.getByText('$150.00 - $250.00')).toBeDefined();
  });

  it('displays single price when min equals max', () => {
    render(<TicketGridView sections={mockSections} />);
    expect(screen.getByText('$50.00')).toBeDefined();
  });

  it('displays dash when prices are null', () => {
    render(<TicketGridView sections={mockSections} />);
    expect(screen.getByText('-')).toBeDefined();
  });

  it('renders color indicator when colorHex is provided', () => {
    render(<TicketGridView sections={mockSections} />);
    expect(screen.getByLabelText('Section color: #FF0000')).toBeDefined();
  });

  it('calls onSectionClick when a section card is clicked', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();
    render(<TicketGridView sections={mockSections} onSectionClick={handleClick} />);

    await user.click(screen.getByText('Floor A'));
    expect(handleClick).toHaveBeenCalledWith(1);
  });
});
