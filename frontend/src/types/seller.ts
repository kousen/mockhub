export interface SellerListing {
  id: number;
  ticketId: number;
  eventSlug: string;
  eventName: string;
  eventDate: string;
  venueName: string;
  sectionName: string;
  rowLabel: string | null;
  seatNumber: string | null;
  ticketType: string;
  listedPrice: number;
  computedPrice: number;
  faceValue: number;
  status: string; // ACTIVE | SOLD | CANCELLED
  listedAt: string;
  createdAt: string;
}

export interface SellListingRequest {
  eventSlug: string;
  sectionName: string;
  rowLabel: string;
  seatNumber: string;
  price: number;
}

export interface UpdatePriceRequest {
  price: number;
}

export interface EarningsSummary {
  totalEarnings: number;
  totalListings: number;
  activeListings: number;
  soldListings: number;
  recentSales: Sale[];
}

export interface Sale {
  orderId: number;
  eventName: string;
  sectionName: string;
  seatInfo: string;
  pricePaid: number;
  soldAt: string;
}
