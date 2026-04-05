import type { EventDetail } from './event';

export interface DashboardStats {
  totalUsers: number;
  totalOrders: number;
  totalRevenue: number;
  activeEvents: number;
  usersTrend: number | null;
  ordersTrend: number | null;
  revenueTrend: number | null;
  eventsTrend: number | null;
}

export interface AdminEvent extends EventDetail {
  totalTicketCount: number;
  soldTicketCount: number;
  totalRevenue: number;
}

export interface AdminUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  enabled: boolean;
  createdAt: string;
  orderCount: number;
}

export interface CreateEventRequest {
  name: string;
  artistName: string | null;
  venueId: number;
  categoryId: number;
  eventDate: string;
  doorsOpenAt: string | null;
  basePrice: number;
  description: string | null;
}

export interface UpdateEventRequest {
  name?: string;
  artistName?: string | null;
  venueId?: number;
  categoryId?: number;
  eventDate?: string;
  doorsOpenAt?: string | null;
  basePrice?: number;
  description?: string | null;
  status?: string;
}

export interface UserRoleUpdate {
  userId: number;
  roles: string[];
}

export interface UserStatusUpdate {
  userId: number;
  enabled: boolean;
}

export interface AdminOrderSummary {
  id: number;
  orderNumber: string;
  userEmail: string;
  userName: string;
  status: string;
  total: number;
  itemCount: number;
  createdAt: string;
}

export interface GenerateTicketsRequest {
  eventId: number;
  sectionName: string;
  rowCount: number;
  seatsPerRow: number;
  ticketType: string;
  basePrice: number;
}

export type { VenueSummary } from './venue';
