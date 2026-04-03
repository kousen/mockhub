import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { AdminLayout } from './AdminLayout';

// AdminLayout uses <Outlet />, which needs a route context.
// We render it directly and just verify sidebar links.
vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router');
  return {
    ...actual,
    Outlet: () => <div data-testid="outlet">Outlet Content</div>,
    useLocation: () => ({ pathname: '/admin' }),
  };
});

describe('AdminLayout', () => {
  it('renders desktop sidebar with navigation links', () => {
    renderWithProviders(<AdminLayout />);

    expect(screen.getByText('Admin')).toBeDefined();

    const dashboardLink = screen.getByRole('link', { name: /Dashboard/i });
    expect(dashboardLink).toBeDefined();
    expect(dashboardLink.getAttribute('href')).toBe('/admin');

    const eventsLink = screen.getByRole('link', { name: /Events/i });
    expect(eventsLink).toBeDefined();
    expect(eventsLink.getAttribute('href')).toBe('/admin/events');

    const usersLink = screen.getByRole('link', { name: /Users/i });
    expect(usersLink).toBeDefined();
    expect(usersLink.getAttribute('href')).toBe('/admin/users');
  });

  it('renders outlet for child routes', () => {
    renderWithProviders(<AdminLayout />);

    expect(screen.getByTestId('outlet')).toBeDefined();
    expect(screen.getByText('Outlet Content')).toBeDefined();
  });

  it('renders menu toggle button', () => {
    renderWithProviders(<AdminLayout />);

    const menuButton = screen.getByRole('button', { name: /Menu/i });
    expect(menuButton).toBeDefined();
  });

  it('toggles mobile sidebar on menu button click', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AdminLayout />);

    const menuButton = screen.getByRole('button', { name: /Menu/i });
    await user.click(menuButton);

    // When sidebar is open, the close overlay appears
    const closeButton = screen.getByRole('button', { name: 'Close sidebar' });
    expect(closeButton).toBeDefined();
  });

  it('closes mobile sidebar on overlay click', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AdminLayout />);

    // Open sidebar
    const menuButton = screen.getByRole('button', { name: /Menu/i });
    await user.click(menuButton);

    // Close via overlay
    const closeOverlay = screen.getByRole('button', { name: 'Close sidebar' });
    await user.click(closeOverlay);

    // Overlay should be gone
    expect(screen.queryByRole('button', { name: 'Close sidebar' })).toBeNull();
  });

  it('closes mobile sidebar on Enter key on overlay', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AdminLayout />);

    // Open sidebar
    const menuButton = screen.getByRole('button', { name: /Menu/i });
    await user.click(menuButton);

    // Focus the overlay and press Enter
    const closeOverlay = screen.getByRole('button', { name: 'Close sidebar' });
    closeOverlay.focus();
    await user.keyboard('{Enter}');

    // Overlay should be gone
    expect(screen.queryByRole('button', { name: 'Close sidebar' })).toBeNull();
  });

  it('closes mobile sidebar on Space key on overlay', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AdminLayout />);

    // Open sidebar
    const menuButton = screen.getByRole('button', { name: /Menu/i });
    await user.click(menuButton);

    // Focus the overlay and press Space
    const closeOverlay = screen.getByRole('button', { name: 'Close sidebar' });
    closeOverlay.focus();
    await user.keyboard(' ');

    // Overlay should be gone
    expect(screen.queryByRole('button', { name: 'Close sidebar' })).toBeNull();
  });
});
