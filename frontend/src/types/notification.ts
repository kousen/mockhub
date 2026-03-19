export type NotificationType =
  | 'ORDER_CONFIRMED'
  | 'ORDER_CANCELLED'
  | 'EVENT_REMINDER'
  | 'PRICE_DROP'
  | 'SYSTEM';

export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  link: string | null;
  isRead: boolean;
  createdAt: string;
}

export interface UnreadCountResponse {
  count: number;
}
