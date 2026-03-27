import type { VenueSummary } from './venue';

export interface EventSummary {
  id: number;
  name: string;
  slug: string;
  artistName: string | null;
  venueName: string;
  city: string;
  eventDate: string;
  minPrice: number | null;
  availableTickets: number;
  primaryImageUrl: string | null;
  categoryName: string;
  isFeatured: boolean;
}

export interface EventDetail {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  artistName: string | null;
  eventDate: string;
  doorsOpenAt: string | null;
  status: string;
  basePrice: number;
  minPrice: number | null;
  maxPrice: number | null;
  totalTickets: number;
  availableTickets: number;
  isFeatured: boolean;
  venue: VenueSummary;
  category: Category;
  tags: Tag[];
  primaryImageUrl: string | null;
  spotifyArtistId: string | null;
}

export interface EventSearchParams {
  q?: string;
  category?: string;
  tags?: string;
  city?: string;
  dateFrom?: string;
  dateTo?: string;
  minPrice?: number;
  maxPrice?: number;
  sort?: 'date' | 'price_asc' | 'price_desc' | 'name' | 'popularity';
  page?: number;
  size?: number;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  icon: string | null;
  sortOrder: number;
}

export interface Tag {
  id: number;
  name: string;
  slug: string;
}

export type { VenueSummary };
