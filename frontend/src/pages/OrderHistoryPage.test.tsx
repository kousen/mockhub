import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { OrderHistoryPage } from './OrderHistoryPage';
import type { OrderSummary } from '@/types/order';
import type { PageResponse } from '@/types/common';

vi.mock('@/hooks/use-orders', () => ({
  useOrders: vi.fn(() => ({
    data: undefined,
    isLoading: false,
  })),
}));

import { useOrders } from '@/hooks/use-orders';

const mockOrders: OrderSummary[] = [
  {
    id: 1,
    orderNumber: 'ORD-001',
    status: 'CONFIRMED',
    total: 275.0,
    itemCount: 2,
    createdAt: '2026-03-20T10:00:00',
  },
  {
    id: 2,
    orderNumber: 'ORD-002',
    status: 'PENDING',
    total: 150.0,
    itemCount: 1,
    createdAt: '2026-03-19T14:30:00',
  },
];

function setOrdersState(data: PageResponse<OrderSummary> | undefined, isLoading = false) {
  vi.mocked(useOrders).mockReturnValue({
    data,
    isLoading,
  } as ReturnType<typeof useOrders>);
}

describe('OrderHistoryPage', () => {
  it('renders order list when orders exist', () => {
    setOrdersState({
      content: mockOrders,
      totalElements: 2,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
    });

    renderWithProviders(<OrderHistoryPage />);

    expect(screen.getByText('My Orders')).toBeDefined();
    expect(screen.getByText('#ORD-001')).toBeDefined();
    expect(screen.getByText('#ORD-002')).toBeDefined();
  });

  it('shows empty state when no orders', () => {
    setOrdersState({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 20,
      number: 0,
      first: true,
      last: true,
    });

    renderWithProviders(<OrderHistoryPage />);

    expect(screen.getByText('No orders yet')).toBeDefined();
    expect(
      screen.getByText('When you purchase tickets, your orders will appear here.'),
    ).toBeDefined();
  });

  it('shows loading skeleton while fetching', () => {
    setOrdersState(undefined, true);

    const { container } = renderWithProviders(<OrderHistoryPage />);

    const skeletons = container.querySelectorAll('[class*="animate-pulse"]');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('displays order summary info (order number, status, total)', () => {
    setOrdersState({
      content: mockOrders,
      totalElements: 2,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
    });

    renderWithProviders(<OrderHistoryPage />);

    expect(screen.getByText('#ORD-001')).toBeDefined();
    expect(screen.getByText('CONFIRMED')).toBeDefined();
    expect(screen.getByText('$275.00')).toBeDefined();
    expect(screen.getByText('#ORD-002')).toBeDefined();
    expect(screen.getByText('PENDING')).toBeDefined();
    expect(screen.getByText('$150.00')).toBeDefined();
  });

  it('shows pagination when multiple pages exist', () => {
    setOrdersState({
      content: mockOrders,
      totalElements: 40,
      totalPages: 2,
      size: 20,
      number: 0,
      first: true,
      last: false,
    });

    renderWithProviders(<OrderHistoryPage />);

    expect(screen.getByText('Previous')).toBeDefined();
    expect(screen.getByText('Next')).toBeDefined();
    expect(screen.getByText('Page 1 of 2')).toBeDefined();
  });
});
