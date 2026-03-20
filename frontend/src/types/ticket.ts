export interface Listing {
  id: number;
  ticketId: number;
  eventSlug: string;
  sectionName: string;
  rowLabel: string | null;
  seatNumber: string | null;
  ticketType: string;
  listedPrice: number;
  computedPrice: number;
  priceMultiplier: number;
  status: string;
  listedAt: string;
  sellerDisplayName: string | null;
}

export interface PriceHistory {
  id: number;
  eventId: number;
  price: number;
  multiplier: number;
  supplyRatio: number;
  demandScore: number | null;
  daysToEvent: number;
  recordedAt: string;
}

export interface SectionAvailability {
  id: number;
  name: string;
  sectionType: string;
  availableCount: number;
  minPrice: number | null;
  maxPrice: number | null;
  colorHex: string | null;
}
