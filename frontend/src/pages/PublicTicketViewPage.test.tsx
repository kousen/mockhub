import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TooltipProvider } from '@/components/ui/tooltip';
import { PublicTicketViewPage } from './PublicTicketViewPage';
import * as publicTicketsApi from '@/api/public-tickets';

vi.mock('@/api/public-tickets');

function renderPage(search = '') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <MemoryRouter initialEntries={[`/tickets/view${search}`]}>
          <Routes>
            <Route path="/tickets/view" element={<PublicTicketViewPage />} />
          </Routes>
        </MemoryRouter>
      </TooltipProvider>
    </QueryClientProvider>,
  );
}

describe('PublicTicketViewPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('shows error when no token provided', () => {
    renderPage();
    expect(screen.getByText('No ticket token provided')).toBeDefined();
  });

  it('shows loading state while fetching', () => {
    vi.mocked(publicTicketsApi.getPublicOrderView).mockReturnValue(new Promise(() => {}));
    renderPage('?token=test-token');
    expect(screen.getByText('Loading your tickets...')).toBeDefined();
  });

  it('renders ticket data on success', async () => {
    vi.mocked(publicTicketsApi.getPublicOrderView).mockResolvedValue({
      orderNumber: 'MH-20260322-0003',
      status: 'CONFIRMED',
      eventName: 'Yo-Yo Ma - Bach Cello Suites',
      eventDate: 'Sunday, March 22, 2026 at 7:00 PM',
      venueName: 'Carnegie Hall',
      venueLocation: 'New York, NY',
      tickets: [
        {
          ticketId: 1,
          sectionName: 'Orchestra',
          rowLabel: 'A',
          seatNumber: '7',
          ticketType: 'STANDARD',
          qrCodeUrl: '/api/v1/tickets/MH-20260322-0003/1/qr?token=test-token',
        },
      ],
    });

    renderPage('?token=test-token');

    await waitFor(() => {
      expect(screen.getByText('Yo-Yo Ma - Bach Cello Suites')).toBeDefined();
    });

    expect(screen.getByText(/Carnegie Hall/)).toBeDefined();
    expect(screen.getByText('Orchestra')).toBeDefined();
    expect(screen.getByText(/Row A/)).toBeDefined();
    expect(screen.getByText('Show this QR code at the venue entrance.')).toBeDefined();

    const qrImage = screen.getByAltText('QR code for ticket 1');
    expect(qrImage.getAttribute('src')).toBe(
      '/api/v1/tickets/MH-20260322-0003/1/qr?token=test-token',
    );
  });

  it('shows error on API failure', async () => {
    vi.mocked(publicTicketsApi.getPublicOrderView).mockRejectedValue(new Error('fail'));

    renderPage('?token=bad-token');

    await waitFor(() => {
      expect(
        screen.getByText('Unable to load tickets. The link may be invalid or expired.'),
      ).toBeDefined();
    });
  });
});
