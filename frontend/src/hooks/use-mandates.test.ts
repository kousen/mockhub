import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement, type ReactNode } from 'react';
import { useMyMandates, useCreateMandate, useRevokeMandate } from './use-mandates';

vi.mock('@/api/mandates', () => ({
  getMyMandates: vi.fn().mockResolvedValue([]),
  createMandate: vi.fn().mockResolvedValue({ mandateId: 'test-mandate' }),
  revokeMandate: vi.fn().mockResolvedValue(undefined),
}));

vi.mock('@/stores/auth-store', () => ({
  useAuthStore: vi.fn((selector: (state: { isAuthenticated: boolean }) => boolean) =>
    selector({ isAuthenticated: true }),
  ),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe('use-mandates hooks', () => {
  beforeEach(() => vi.clearAllMocks());

  it('useMyMandates fetches mandates', async () => {
    const { result } = renderHook(() => useMyMandates(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });

  it('useCreateMandate calls createMandate', async () => {
    const mandatesApi = await import('@/api/mandates');
    const { result } = renderHook(() => useCreateMandate(), { wrapper: createWrapper() });

    result.current.mutate({
      agentId: 'claude-desktop',
      scope: 'PURCHASE',
      maxSpendPerTransaction: 200,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(mandatesApi.createMandate).toHaveBeenCalledWith(
      expect.objectContaining({ agentId: 'claude-desktop' }),
    );
  });

  it('useRevokeMandate calls revokeMandate', async () => {
    const mandatesApi = await import('@/api/mandates');
    const { result } = renderHook(() => useRevokeMandate(), { wrapper: createWrapper() });

    result.current.mutate('mandate-123');

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(mandatesApi.revokeMandate).toHaveBeenCalledWith('mandate-123');
  });
});
