export type NotificationType =
  | 'PRICE_DROP'
  | 'EVENT_REMINDER'
  | 'ORDER_UPDATE'
  | 'NEW_LISTING'
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
