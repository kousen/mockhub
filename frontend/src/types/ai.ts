export interface ChatRequest {
  message: string;
  conversationId: number | null;
}

export interface ChatResponse {
  conversationId: number;
  message: string;
  timestamp: string;
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
}

export interface Recommendation {
  eventId: number;
  eventName: string;
  eventSlug: string;
  venueName: string;
  city: string;
  eventDate: string;
  minPrice: number;
  relevanceScore: number;
  reason: string;
}

export type PriceTrend = 'RISING' | 'FALLING' | 'STABLE';

export interface PricePrediction {
  eventSlug: string;
  currentPrice: number;
  predictedPrice: number;
  trend: PriceTrend;
  confidence: number;
  predictedAt: string;
}
