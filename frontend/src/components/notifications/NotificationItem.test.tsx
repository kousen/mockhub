import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { NotificationItem } from './NotificationItem';
import type { Notification } from '@/types/notification';

function createNotification(overrides: Partial<Notification> = {}): Notification {
  return {
    id: 1,
    type: 'ORDER_CONFIRMED',
    title: 'Order Confirmed',
    message: 'Your order #ABC123 has been confirmed',
    link: '/orders/ABC123/confirmation',
    isRead: false,
    createdAt: new Date().toISOString(),
    ...overrides,
  };
}

describe('NotificationItem', () => {
  it('renders notification title and message', () => {
    const notification = createNotification();
    renderWithProviders(<NotificationItem notification={notification} onMarkAsRead={vi.fn()} />);

    expect(screen.getByText('Order Confirmed')).toBeDefined();
    expect(screen.getByText('Your order #ABC123 has been confirmed')).toBeDefined();
  });

  it('calls onMarkAsRead when clicking an unread notification', async () => {
    const user = userEvent.setup();
    const onMarkAsRead = vi.fn();
    const notification = createNotification({ isRead: false });
    renderWithProviders(
      <NotificationItem notification={notification} onMarkAsRead={onMarkAsRead} />,
    );

    await user.click(screen.getByRole('button'));

    expect(onMarkAsRead).toHaveBeenCalledWith(1);
  });

  it('does not call onMarkAsRead when clicking an already-read notification', async () => {
    const user = userEvent.setup();
    const onMarkAsRead = vi.fn();
    const notification = createNotification({ isRead: true });
    renderWithProviders(
      <NotificationItem notification={notification} onMarkAsRead={onMarkAsRead} />,
    );

    await user.click(screen.getByRole('button'));

    expect(onMarkAsRead).not.toHaveBeenCalled();
  });

  it('calls onNavigate with link when clicking a notification with a link', async () => {
    const user = userEvent.setup();
    const onNavigate = vi.fn();
    const notification = createNotification({ link: '/orders/ABC123/confirmation' });
    renderWithProviders(
      <NotificationItem
        notification={notification}
        onMarkAsRead={vi.fn()}
        onNavigate={onNavigate}
      />,
    );

    await user.click(screen.getByRole('button'));

    expect(onNavigate).toHaveBeenCalledWith('/orders/ABC123/confirmation');
  });

  it('does not call onNavigate when notification has no link', async () => {
    const user = userEvent.setup();
    const onNavigate = vi.fn();
    const notification = createNotification({ link: null });
    renderWithProviders(
      <NotificationItem
        notification={notification}
        onMarkAsRead={vi.fn()}
        onNavigate={onNavigate}
      />,
    );

    await user.click(screen.getByRole('button'));

    expect(onNavigate).not.toHaveBeenCalled();
  });

  it('shows unread indicator for unread notifications', () => {
    const notification = createNotification({ isRead: false });
    renderWithProviders(<NotificationItem notification={notification} onMarkAsRead={vi.fn()} />);

    const title = screen.getByText('Order Confirmed');
    expect(title.className).toContain('font-semibold');
  });

  it('does not show unread indicator for read notifications', () => {
    const notification = createNotification({ isRead: true });
    renderWithProviders(<NotificationItem notification={notification} onMarkAsRead={vi.fn()} />);

    const title = screen.getByText('Order Confirmed');
    expect(title.className).toContain('font-medium');
    expect(title.className).not.toContain('font-semibold');
  });
});
