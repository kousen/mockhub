import { describe, it, expect, vi } from 'vitest';
import { login, register, refreshToken, getMe, updateMe, exchangeOAuth2Code, getLinkedProviders, unlinkProvider } from './auth';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock-data' }),
    post: vi.fn().mockResolvedValue({ data: 'mock-data' }),
    put: vi.fn().mockResolvedValue({ data: 'mock-data' }),
    delete: vi.fn().mockResolvedValue({}),
  },
}));

describe('auth API', () => {
  it('login posts credentials', async () => {
    const client = (await import('./client')).default;
    const result = await login({ email: 'a@b.com', password: 'pass' });
    expect(client.post).toHaveBeenCalledWith('/auth/login', { email: 'a@b.com', password: 'pass' });
    expect(result).toBe('mock-data');
  });

  it('register posts user data', async () => {
    const client = (await import('./client')).default;
    await register({ email: 'a@b.com', password: 'pass', firstName: 'A', lastName: 'B' });
    expect(client.post).toHaveBeenCalledWith('/auth/register', expect.objectContaining({ email: 'a@b.com' }));
  });

  it('refreshToken posts to /auth/refresh', async () => {
    const client = (await import('./client')).default;
    await refreshToken();
    expect(client.post).toHaveBeenCalledWith('/auth/refresh');
  });

  it('getMe gets /auth/me', async () => {
    const client = (await import('./client')).default;
    await getMe();
    expect(client.get).toHaveBeenCalledWith('/auth/me');
  });

  it('updateMe puts to /auth/me', async () => {
    const client = (await import('./client')).default;
    await updateMe({ firstName: 'New' });
    expect(client.put).toHaveBeenCalledWith('/auth/me', { firstName: 'New' });
  });

  it('exchangeOAuth2Code posts with code', async () => {
    const client = (await import('./client')).default;
    await exchangeOAuth2Code('abc123');
    expect(client.post).toHaveBeenCalledWith('/auth/oauth2/exchange?code=abc123');
  });

  it('getLinkedProviders gets providers', async () => {
    const client = (await import('./client')).default;
    await getLinkedProviders();
    expect(client.get).toHaveBeenCalledWith('/auth/me/providers');
  });

  it('unlinkProvider deletes provider', async () => {
    const client = (await import('./client')).default;
    await unlinkProvider('google');
    expect(client.delete).toHaveBeenCalledWith('/auth/me/providers/google');
  });
});
