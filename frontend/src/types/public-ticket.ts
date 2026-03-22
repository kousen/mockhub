export interface PublicTicketView {
  ticketId: number;
  sectionName: string;
  rowLabel: string | null;
  seatNumber: string | null;
  ticketType: string;
  qrCodeUrl: string;
}

export interface PublicOrderView {
  orderNumber: string;
  status: string;
  eventName: string;
  eventDate: string;
  venueName: string;
  venueLocation: string;
  tickets: PublicTicketView[];
}
