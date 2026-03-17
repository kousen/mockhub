import type { EventSummary } from './event';

export interface Favorite {
  id: number;
  eventId: number;
  event: EventSummary;
  createdAt: string;
}
