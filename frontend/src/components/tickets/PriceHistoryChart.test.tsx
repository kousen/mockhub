import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PriceHistoryChart } from './PriceHistoryChart';
import type { PriceHistory } from '@/types/ticket';

// Mock Recharts components since jsdom doesn't support SVG rendering
vi.mock('recharts', () => ({
  LineChart: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="line-chart">{children}</div>
  ),
  Line: () => <div data-testid="line" />,
  XAxis: () => <div data-testid="x-axis" />,
  YAxis: () => <div data-testid="y-axis" />,
  CartesianGrid: () => <div data-testid="cartesian-grid" />,
  Tooltip: () => <div data-testid="tooltip" />,
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="responsive-container">{children}</div>
  ),
}));

const mockPriceData: PriceHistory[] = [
  {
    id: 1,
    eventId: 10,
    price: 100.0,
    multiplier: 1.0,
    supplyRatio: 0.8,
    demandScore: null,
    daysToEvent: 30,
    recordedAt: '2026-05-01T12:00:00Z',
  },
  {
    id: 2,
    eventId: 10,
    price: 120.0,
    multiplier: 1.2,
    supplyRatio: 0.6,
    demandScore: null,
    daysToEvent: 20,
    recordedAt: '2026-05-11T12:00:00Z',
  },
  {
    id: 3,
    eventId: 10,
    price: 150.0,
    multiplier: 1.5,
    supplyRatio: 0.3,
    demandScore: null,
    daysToEvent: 10,
    recordedAt: '2026-05-21T12:00:00Z',
  },
];

describe('PriceHistoryChart', () => {
  it('renders loading skeleton when isLoading is true', () => {
    const { container } = render(<PriceHistoryChart data={[]} isLoading={true} />);
    const skeletons = container.querySelectorAll('[data-slot="skeleton"]');
    expect(skeletons.length).toBe(1);
  });

  it('renders empty state when data is empty', () => {
    render(<PriceHistoryChart data={[]} />);
    expect(screen.getByText('No price history')).toBeDefined();
    expect(
      screen.getByText('Price data will appear as tickets are bought and sold.'),
    ).toBeDefined();
  });

  it('renders the chart when data is provided', () => {
    render(<PriceHistoryChart data={mockPriceData} />);
    expect(screen.getByTestId('responsive-container')).toBeDefined();
    expect(screen.getByTestId('line-chart')).toBeDefined();
    expect(screen.getByTestId('line')).toBeDefined();
  });

  it('does not show empty state when data is provided', () => {
    render(<PriceHistoryChart data={mockPriceData} />);
    expect(screen.queryByText('No price history')).toBeNull();
  });
});
