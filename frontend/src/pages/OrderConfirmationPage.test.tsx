import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { OrderConfirmationPage } from './OrderConfirmationPage';
import type { Order } from '@/types/order';

vi.mock('react-router', async () => {
  const actual = await vi.importActual<typeof import('react-router')>('react-router');
  return {
    ...actual,
    useParams: vi.fn(() => ({ orderNumber: 'ORD-001' })),
  };
});

vi.mock('@/hooks/use-orders', () => ({
  useOrder: vi.fn(() => ({
    data: undefined,
    isLoading: false,
  })),
  useDownloadTicket: vi.fn(() => ({
    mutate: vi.fn(),
    isPending: false,
  })),
}));

import { useOrder } from '@/hooks/use-orders';

const mockOrder: Order = {
  id: 1,
  orderNumber: 'ORD-001',
  status: 'CONFIRMED',
  subtotal: 250.0,
  serviceFee: 25.0,
  total: 275.0,
  paymentMethod: 'MOCK',
  confirmedAt: '2026-03-20T10:05:00',
  createdAt: '2026-03-20T10:00:00',
  agentId: null,
  mandateId: null,
  items: [
    {
      id: 201,
      ticketId: 301,
      eventName: 'Taylor Swift - Eras Tour',
      eventSlug: 'taylor-swift-eras-tour',
      sectionName: 'Floor A',
      rowLabel: 'Row 5',
      seatNumber: '12',
      ticketType: 'STANDARD',
      pricePaid: 250.0,
    },
  ],
};

function setOrderState(data: Order | undefined, isLoading = false) {
  vi.mocked(useOrder).mockReturnValue({
    data,
    isLoading,
  } as unknown as ReturnType<typeof useOrder>);
}

describe('OrderConfirmationPage', () => {
  it('renders confirmation details when order is loaded', () => {
    setOrderState(mockOrder);

    renderWithProviders(<OrderConfirmationPage />);

    expect(screen.getByText('Order Confirmed!')).toBeDefined();
    expect(screen.getByText('#ORD-001')).toBeDefined();
    expect(screen.getByText('CONFIRMED')).toBeDefined();
    expect(screen.getByText('$275.00')).toBeDefined();
    // $250.00 appears for both the item price and subtotal
    expect(screen.getAllByText('$250.00').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('$25.00')).toBeDefined();
    expect(screen.getByText('MOCK')).toBeDefined();
    expect(screen.getByText('Tickets (1)')).toBeDefined();
  });

  it('shows "Order Details" title for non-confirmed orders', () => {
    setOrderState({ ...mockOrder, status: 'PENDING', confirmedAt: null });

    renderWithProviders(<OrderConfirmationPage />);

    expect(screen.getByText('Order Details')).toBeDefined();
  });

  it('shows loading state', () => {
    setOrderState(undefined, true);

    const { container } = renderWithProviders(<OrderConfirmationPage />);

    const skeletons = container.querySelectorAll('[class*="animate-pulse"]');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('handles not found state when order is undefined and not loading', () => {
    setOrderState(undefined);

    renderWithProviders(<OrderConfirmationPage />);

    expect(screen.getByText('Order Not Found')).toBeDefined();
    expect(screen.getByText('The order you are looking for does not exist.')).toBeDefined();
    expect(screen.getByText('View Orders')).toBeDefined();
  });

  it('displays action buttons for navigation', () => {
    setOrderState(mockOrder);

    renderWithProviders(<OrderConfirmationPage />);

    expect(screen.getByText('View Order History')).toBeDefined();
    expect(screen.getByText('Continue Browsing')).toBeDefined();
  });

  it('shows agent attribution badge for agent-initiated orders', () => {
    setOrderState({
      ...mockOrder,
      agentId: 'claude-desktop',
      mandateId: 'mandate-001',
    });

    renderWithProviders(<OrderConfirmationPage />);

    expect(screen.getByText(/claude-desktop/)).toBeDefined();
  });

  it('does not show agent badge for human orders', () => {
    setOrderState({
      ...mockOrder,
      agentId: null,
      mandateId: null,
    });

    renderWithProviders(<OrderConfirmationPage />);

    expect(screen.queryByText(/Purchased by agent/)).toBeNull();
  });
});
