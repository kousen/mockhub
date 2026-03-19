import { useMutation, useQuery } from '@tanstack/react-query';
import * as aiApi from '@/api/ai';
import type { ChatRequest } from '@/types/ai';

/**
 * Mutation hook for sending chat messages to the AI assistant.
 */
export function useChat() {
  return useMutation({
    mutationFn: (data: ChatRequest) => aiApi.sendChatMessage(data),
  });
}

/**
 * Hook for fetching AI-powered event recommendations.
 * Uses a longer stale time since recommendations change infrequently.
 */
export function useRecommendations() {
  return useQuery({
    queryKey: ['ai', 'recommendations'],
    queryFn: () => aiApi.getRecommendations(),
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: false, // Don't retry if AI is unavailable
  });
}

/**
 * Hook for fetching AI price prediction for a specific event.
 */
export function usePricePrediction(slug: string) {
  return useQuery({
    queryKey: ['ai', 'price-prediction', slug],
    queryFn: () => aiApi.getPricePrediction(slug),
    enabled: slug.length > 0,
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: false, // Don't retry if AI is unavailable
  });
}
