import apiClient from './client';
import type {
  ChatRequest,
  ChatResponse,
  RecommendationsResponse,
  PricePrediction,
} from '@/types/ai';
import { isAxiosError } from 'axios';

/**
 * Send a chat message to the AI assistant.
 * Returns null if AI features are unavailable (503).
 */
export async function sendChatMessage(data: ChatRequest): Promise<ChatResponse | null> {
  try {
    const response = await apiClient.post<ChatResponse>('/chat', data);
    return response.data;
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 503) {
      return null;
    }
    throw error;
  }
}

/**
 * Fetch AI-powered event recommendations for the current user.
 * Returns empty response if AI features are unavailable (503).
 */
export async function getRecommendations(city?: string): Promise<RecommendationsResponse> {
  try {
    const params = city ? { city } : undefined;
    const response = await apiClient.get<RecommendationsResponse>('/recommendations', {
      params,
    });
    return response.data;
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 503) {
      return { recommendations: [], spotifyConnected: false, scopeUpgradeNeeded: false };
    }
    throw error;
  }
}

/**
 * Fetch AI price prediction for a specific event.
 * Returns null if AI features are unavailable (503).
 */
export async function getPricePrediction(slug: string): Promise<PricePrediction | null> {
  try {
    const response = await apiClient.get<PricePrediction>(`/events/${slug}/predicted-price`);
    return response.data;
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 503) {
      return null;
    }
    throw error;
  }
}
