export interface CartItem {
  id: number;
  listingId: number;
  eventName: string;
  eventSlug: string;
  sectionName: string;
  rowLabel: string | null;
  seatNumber: string | null;
  ticketType: string;
  priceAtAdd: number;
  currentPrice: number;
  addedAt: string;
}

export interface Cart {
  id: number;
  userId: number;
  items: CartItem[];
  subtotal: number;
  itemCount: number;
  expiresAt: string | null;
}

export interface AddToCartRequest {
  listingId: number;
}
