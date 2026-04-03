import { describe, expect, it, vi } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { SeatSelector } from './SeatSelector';
import { renderWithProviders } from '@/test/test-utils';
import type { SectionAvailability } from '@/types/ticket';

const mockSections: SectionAvailability[] = [
  {
    sectionId: 1,
    sectionName: 'Floor',
    sectionType: 'FLOOR',
    totalTickets: 200,
    availableTickets: 150,
    minPrice: 75,
    maxPrice: 100,
    colorHex: '#FF4444',
    svgPathId: 'floor',
    svgX: 0,
    svgY: 0,
    svgWidth: 100,
    svgHeight: 50,
  },
  {
    sectionId: 2,
    sectionName: 'VIP',
    sectionType: 'VIP',
    totalTickets: 50,
    availableTickets: 0,
    minPrice: 200,
    maxPrice: 200,
    colorHex: '#FFD700',
    svgPathId: 'vip',
    svgX: 0,
    svgY: 0,
    svgWidth: 100,
    svgHeight: 50,
  },
  {
    sectionId: 3,
    sectionName: 'Balcony',
    sectionType: 'UPPER',
    totalTickets: 300,
    availableTickets: 250,
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

describe('SeatSelector', () => {
  it('renders loading skeletons when isLoading is true', () => {
    renderWithProviders(<SeatSelector sections={[]} isLoading={true} />);
    const skeletons = document.querySelectorAll('.animate-pulse');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('renders empty state when sections is empty', () => {
    renderWithProviders(<SeatSelector sections={[]} />);
    expect(screen.getByText('No sections available')).toBeDefined();
  });

  it('renders section cards with names', () => {
    renderWithProviders(<SeatSelector sections={mockSections} />);
    expect(screen.getByText('Floor')).toBeDefined();
    expect(screen.getByText('VIP')).toBeDefined();
    expect(screen.getByText('Balcony')).toBeDefined();
  });

  it('shows ticket count badges', () => {
    renderWithProviders(<SeatSelector sections={mockSections} />);
    expect(screen.getByText('150 tickets')).toBeDefined();
    expect(screen.getByText('0 tickets')).toBeDefined();
    expect(screen.getByText('250 tickets')).toBeDefined();
  });

  it('shows price for sections with minPrice', () => {
    renderWithProviders(<SeatSelector sections={mockSections} />);
    expect(screen.getByText('$75.00+')).toBeDefined();
    expect(screen.getByText('$200.00')).toBeDefined();
  });

  it('shows dash for sections without price', () => {
    renderWithProviders(<SeatSelector sections={mockSections} />);
    expect(screen.getByText('-')).toBeDefined();
  });

  it('calls onSectionSelect when available section is clicked', () => {
    const onSelect = vi.fn();
    renderWithProviders(
      <SeatSelector sections={mockSections} selectedSectionId={null} onSectionSelect={onSelect} />,
    );
    fireEvent.click(screen.getByText('Floor'));
    expect(onSelect).toHaveBeenCalledWith(1);
  });

  it('does not call onSectionSelect when unavailable section is clicked', () => {
    const onSelect = vi.fn();
    renderWithProviders(
      <SeatSelector sections={mockSections} selectedSectionId={null} onSectionSelect={onSelect} />,
    );
    fireEvent.click(screen.getByText('VIP'));
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('deselects when clicking already selected section', () => {
    const onSelect = vi.fn();
    renderWithProviders(
      <SeatSelector sections={mockSections} selectedSectionId={1} onSectionSelect={onSelect} />,
    );
    fireEvent.click(screen.getByText('Floor'));
    expect(onSelect).toHaveBeenCalledWith(null);
  });

  it('renders color indicator when colorHex is provided', () => {
    renderWithProviders(<SeatSelector sections={mockSections} />);
    const colorDots = document.querySelectorAll('.rounded-full');
    expect(colorDots.length).toBeGreaterThanOrEqual(2);
  });
});
