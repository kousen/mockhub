export interface OrderItem {
  id: number;
  eventName: string;
  eventSlug: string;
  sectionName: string;
  rowLabel: string | null;
  seatNumber: string | null;
  ticketType: string;
  pricePaid: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  status: string;
  subtotal: number;
  serviceFee: number;
  total: number;
  paymentMethod: string;
  confirmedAt: string | null;
  createdAt: string;
  items: OrderItem[];
}

export interface OrderSummary {
  id: number;
  orderNumber: string;
  status: string;
  total: number;
  itemCount: number;
  createdAt: string;
}

export interface CheckoutRequest {
  paymentMethod: 'MOCK' | 'STRIPE';
}

export interface PaymentIntent {
  paymentIntentId: string;
  clientSecret: string | null;
  amount: number;
  currency: string;
}

export interface PaymentConfirmation {
  paymentIntentId: string;
  status: string;
  orderNumber: string;
}
