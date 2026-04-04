import { describe, it, expect, vi } from 'vitest';
import { sendChatMessage, getRecommendations, getPricePrediction } from './ai';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock' }),
    post: vi.fn().mockResolvedValue({ data: 'mock' }),
  },
}));

vi.mock('axios', () => ({
  isAxiosError: vi.fn().mockReturnValue(false),
}));

describe('ai API', () => {
  it('sendChatMessage posts to /chat', async () => {
    const client = (await import('./client')).default;
    const result = await sendChatMessage({ message: 'Hello', conversationId: null });
    expect(client.post).toHaveBeenCalledWith('/chat', { message: 'Hello', conversationId: null });
    expect(result).toBe('mock');
  });

  it('getRecommendations without city', async () => {
    const client = (await import('./client')).default;
    await getRecommendations();
    expect(client.get).toHaveBeenCalledWith('/recommendations', { params: undefined });
  });

  it('getRecommendations with city', async () => {
    const client = (await import('./client')).default;
    await getRecommendations('NYC');
    expect(client.get).toHaveBeenCalledWith('/recommendations', { params: { city: 'NYC' } });
  });

  it('getPricePrediction calls /events/:slug/predicted-price', async () => {
    const client = (await import('./client')).default;
    await getPricePrediction('test-event');
    expect(client.get).toHaveBeenCalledWith('/events/test-event/predicted-price');
  });

  it('sendChatMessage returns null on 503', async () => {
    const client = (await import('./client')).default;
    const { isAxiosError } = await import('axios');
    vi.mocked(isAxiosError).mockReturnValue(true);
    vi.mocked(client.post).mockRejectedValueOnce({ response: { status: 503 } });

    const result = await sendChatMessage({ message: 'test', conversationId: null });
    expect(result).toBeNull();

    vi.mocked(isAxiosError).mockReturnValue(false);
  });

  it('getRecommendations returns fallback on 503', async () => {
    const client = (await import('./client')).default;
    const { isAxiosError } = await import('axios');
    vi.mocked(isAxiosError).mockReturnValue(true);
    vi.mocked(client.get).mockRejectedValueOnce({ response: { status: 503 } });

    const result = await getRecommendations();
    expect(result).toEqual({
      recommendations: [],
      spotifyConnected: false,
      scopeUpgradeNeeded: false,
    });

    vi.mocked(isAxiosError).mockReturnValue(false);
  });

  it('getPricePrediction returns null on 503', async () => {
    const client = (await import('./client')).default;
    const { isAxiosError } = await import('axios');
    vi.mocked(isAxiosError).mockReturnValue(true);
    vi.mocked(client.get).mockRejectedValueOnce({ response: { status: 503 } });

    const result = await getPricePrediction('test');
    expect(result).toBeNull();

    vi.mocked(isAxiosError).mockReturnValue(false);
  });
});
