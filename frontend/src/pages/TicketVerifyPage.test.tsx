import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import { TicketVerifyPage } from './TicketVerifyPage';

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: Object.assign(
    vi.fn((selector) => {
      const state = { isAuthenticated: false, user: null, accessToken: null };
      return selector(state);
    }),
    {
      getState: () => ({ accessToken: null }),
    },
  ),
}));

vi.mock('@/stores/cart-store', () => ({
  useCartStore: vi.fn((selector) => {
    const state = { itemCount: 0, openDrawer: vi.fn() };
    return selector(state);
  }),
}));

import apiClient from '@/api/client';
vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

const mockedGet = vi.mocked(apiClient.get);

describe('TicketVerifyPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows error when no token is provided', () => {
    renderWithProviders(<TicketVerifyPage />, { route: '/tickets/verify' });
    expect(screen.getByText('Verification Error')).toBeDefined();
    expect(screen.getByText('No ticket token provided')).toBeDefined();
  });

  it('shows loading state while verifying', () => {
    mockedGet.mockReturnValue(new Promise(() => {})); // never resolves
    renderWithProviders(<TicketVerifyPage />, { route: '/tickets/verify?token=abc123' });
    expect(screen.getByText('Verifying ticket...')).toBeDefined();
  });

  it('shows valid ticket result', async () => {
    mockedGet.mockResolvedValue({
      data: {
        valid: true,
        orderNumber: 'MH-20260315-0001',
        ticketId: 42,
        eventSlug: 'rock-festival',
        sectionName: 'Floor A',
        rowLabel: 'R1',
        seatNumber: '15',
        alreadyScanned: false,
        scannedAt: null,
        message: 'Ticket verified successfully',
      },
    });

    renderWithProviders(<TicketVerifyPage />, { route: '/tickets/verify?token=valid-token' });

    await waitFor(() => {
      expect(screen.getByText('Ticket Valid')).toBeDefined();
    });
    expect(screen.getByText('Ticket verified successfully')).toBeDefined();
    expect(screen.getByText('MH-20260315-0001')).toBeDefined();
    expect(screen.getByText('Floor A')).toBeDefined();
    expect(screen.getByText('R1')).toBeDefined();
    expect(screen.getByText('15')).toBeDefined();
  });

  it('shows already scanned warning', async () => {
    mockedGet.mockResolvedValue({
      data: {
        valid: true,
        orderNumber: 'MH-20260315-0001',
        ticketId: 42,
        eventSlug: 'rock-festival',
        sectionName: 'Floor A',
        rowLabel: 'R1',
        seatNumber: '15',
        alreadyScanned: true,
        scannedAt: '2026-03-20T14:30:00Z',
        message: 'Ticket was already scanned',
      },
    });

    renderWithProviders(<TicketVerifyPage />, { route: '/tickets/verify?token=scanned-token' });

    await waitFor(() => {
      expect(screen.getByText('Already Scanned')).toBeDefined();
    });
    expect(screen.getByText('Ticket was already scanned')).toBeDefined();
    expect(screen.getByText(/First scanned at/)).toBeDefined();
  });

  it('shows invalid ticket result', async () => {
    mockedGet.mockResolvedValue({
      data: {
        valid: false,
        orderNumber: null,
        ticketId: null,
        eventSlug: null,
        sectionName: null,
        rowLabel: null,
        seatNumber: null,
        alreadyScanned: false,
        scannedAt: null,
        message: 'Invalid or expired ticket token',
      },
    });

    renderWithProviders(<TicketVerifyPage />, { route: '/tickets/verify?token=bad-token' });

    await waitFor(() => {
      expect(screen.getByText('Invalid Ticket')).toBeDefined();
    });
    expect(screen.getByText('Invalid or expired ticket token')).toBeDefined();
  });

  it('shows error state when API call fails', async () => {
    mockedGet.mockRejectedValue(new Error('Network error'));

    renderWithProviders(<TicketVerifyPage />, { route: '/tickets/verify?token=fail-token' });

    await waitFor(() => {
      expect(screen.getByText('Verification Error')).toBeDefined();
    });
    expect(screen.getByText('Failed to verify ticket. Please try again.')).toBeDefined();
  });

  it('renders back to MockHub link', async () => {
    mockedGet.mockResolvedValue({
      data: {
        valid: true,
        orderNumber: 'MH-001',
        ticketId: 1,
        eventSlug: 'test',
        sectionName: 'A',
        rowLabel: null,
        seatNumber: null,
        alreadyScanned: false,
        scannedAt: null,
        message: 'OK',
      },
    });

    renderWithProviders(<TicketVerifyPage />, { route: '/tickets/verify?token=t' });

    await waitFor(() => {
      expect(screen.getByText('Ticket Valid')).toBeDefined();
    });
    expect(screen.getByRole('link', { name: 'Back to MockHub' })).toBeDefined();
  });
});
