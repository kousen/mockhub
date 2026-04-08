import { describe, it, expect, vi } from 'vitest';
import { getMyMandates, createMandate, revokeMandate } from './mandates';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock' }),
    post: vi.fn().mockResolvedValue({ data: 'mock' }),
    delete: vi.fn().mockResolvedValue({}),
  },
}));

describe('mandates API', () => {
  it('getMyMandates calls /my/mandates', async () => {
    const client = (await import('./client')).default;
    await getMyMandates();
    expect(client.get).toHaveBeenCalledWith('/my/mandates');
  });

  it('createMandate posts to /my/mandates', async () => {
    const client = (await import('./client')).default;
    await createMandate({
      agentId: 'claude-desktop',
      scope: 'PURCHASE',
      maxSpendPerTransaction: 200,
    });
    expect(client.post).toHaveBeenCalledWith(
      '/my/mandates',
      expect.objectContaining({ agentId: 'claude-desktop' }),
    );
  });

  it('revokeMandate deletes /my/mandates/{mandateId}', async () => {
    const client = (await import('./client')).default;
    await revokeMandate('mandate-123');
    expect(client.delete).toHaveBeenCalledWith('/my/mandates/mandate-123');
  });
});
