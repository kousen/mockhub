import { useMutation, useQuery } from '@tanstack/react-query';
import * as aiApi from '@/api/ai';
import type { ChatRequest } from '@/types/ai';
import { useAuthStore } from '@/stores/auth-store';

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
 * Returns the full response including Spotify connection status.
 * Uses a longer stale time since recommendations change infrequently.
 */
export function useRecommendations(city?: string) {
  const user = useAuthStore((state) => state.user);
  return useQuery({
    queryKey: ['ai', 'recommendations', user?.id ?? 'anonymous', city ?? 'all'],
    queryFn: () => aiApi.getRecommendations(city),
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
