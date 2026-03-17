import { Bell, DollarSign, Calendar, Package, Tag, Info } from 'lucide-react';
import { cn } from '@/lib/utils';
import { formatRelativeTime } from '@/lib/formatters';
import type { Notification, NotificationType } from '@/types/notification';

interface NotificationItemProps {
  notification: Notification;
  onMarkAsRead: (id: number) => void;
}

const typeIcons: Record<NotificationType, React.ElementType> = {
  PRICE_DROP: DollarSign,
  EVENT_REMINDER: Calendar,
  ORDER_UPDATE: Package,
  NEW_LISTING: Tag,
  SYSTEM: Info,
};

const typeColors: Record<NotificationType, string> = {
  PRICE_DROP: 'text-green-500',
  EVENT_REMINDER: 'text-blue-500',
  ORDER_UPDATE: 'text-orange-500',
  NEW_LISTING: 'text-purple-500',
  SYSTEM: 'text-muted-foreground',
};

/**
 * A single notification row with icon, title, message, timestamp,
 * and read/unread styling. Clicking marks it as read.
 */
export function NotificationItem({ notification, onMarkAsRead }: NotificationItemProps) {
  const Icon = typeIcons[notification.type] ?? Bell;
  const iconColor = typeColors[notification.type] ?? 'text-muted-foreground';

  const handleClick = () => {
    if (!notification.isRead) {
      onMarkAsRead(notification.id);
    }
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      className={cn(
        'flex w-full items-start gap-3 rounded-md px-3 py-2 text-left transition-colors hover:bg-muted/50',
        !notification.isRead && 'bg-muted/30',
      )}
    >
      <div className={cn('mt-0.5 shrink-0', iconColor)}>
        <Icon className="h-4 w-4" />
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-2">
          <p
            className={cn(
              'text-sm leading-tight',
              !notification.isRead ? 'font-semibold' : 'font-medium',
            )}
          >
            {notification.title}
          </p>
          {!notification.isRead && (
            <span className="mt-1 h-2 w-2 shrink-0 rounded-full bg-primary" />
          )}
        </div>
        <p className="mt-0.5 line-clamp-2 text-xs text-muted-foreground">{notification.message}</p>
        <p className="mt-1 text-xs text-muted-foreground/70">
          {formatRelativeTime(notification.createdAt)}
        </p>
      </div>
    </button>
  );
}
