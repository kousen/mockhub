import { http, HttpResponse } from 'msw';
import { server } from '@/test/mocks/server';
import { useAuthStore } from '@/stores/auth-store';
import type { UserDto } from '@/types/auth';
import apiClient from '@/api/client';

const API_BASE = 'http://localhost:8080/api/v1';

const mockUser: UserDto = {
  id: 1,
  email: 'test@example.com',
  firstName: 'John',
  lastName: 'Doe',
  phone: null,
  avatarUrl: null,
  roles: ['ROLE_BUYER'],
};

describe('apiClient interceptors', () => {
  const originalBaseURL = apiClient.defaults.baseURL;

  beforeAll(() => {
    // Override the baseURL so Axios produces absolute URLs that MSW can intercept in jsdom
    apiClient.defaults.baseURL = API_BASE;
  });

  afterAll(() => {
    apiClient.defaults.baseURL = originalBaseURL;
  });

  beforeEach(() => {
    useAuthStore.setState({
      user: null,
      accessToken: null,
      isAuthenticated: false,
    });

    // Stub window.location to prevent jsdom navigation errors on redirect
    Object.defineProperty(window, 'location', {
      value: { href: 'http://localhost:3000', origin: 'http://localhost:3000' },
      writable: true,
      configurable: true,
    });
  });

  describe('request interceptor', () => {
    it('adds Bearer token when auth store has token', async () => {
      useAuthStore.getState().login('my-jwt-token', mockUser);

      let capturedAuthHeader: string | null = null;

      server.use(
        http.get(`${API_BASE}/test-endpoint`, ({ request }) => {
          capturedAuthHeader = request.headers.get('Authorization');
          return HttpResponse.json({ ok: true });
        }),
      );

      await apiClient.get('/test-endpoint');

      expect(capturedAuthHeader).toBe('Bearer my-jwt-token');
    });

    it('does not add Authorization header when no token', async () => {
      let capturedAuthHeader: string | null = null;

      server.use(
        http.get(`${API_BASE}/test-endpoint`, ({ request }) => {
          capturedAuthHeader = request.headers.get('Authorization');
          return HttpResponse.json({ ok: true });
        }),
      );

      await apiClient.get('/test-endpoint');

      expect(capturedAuthHeader).toBeNull();
    });
  });

  describe('response interceptor', () => {
    it('does not trigger refresh for 401 on public GET /events', async () => {
      server.use(
        http.get(`${API_BASE}/events`, () => {
          return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
        }),
      );

      await expect(apiClient.get('/events')).rejects.toThrow();

      // Auth state should not have been touched (no refresh/logout happened)
      expect(useAuthStore.getState().isAuthenticated).toBe(false);
    });

    it('triggers refresh and retries for 401 on protected endpoint', async () => {
      useAuthStore.getState().login('expired-token', mockUser);

      let orderRequestCount = 0;

      server.use(
        http.get(`${API_BASE}/orders`, () => {
          orderRequestCount++;
          if (orderRequestCount === 1) {
            return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
          }
          return HttpResponse.json({ content: [], totalElements: 0 });
        }),
        http.post(`${API_BASE}/auth/refresh`, () => {
          return HttpResponse.json({ accessToken: 'new-token' });
        }),
      );

      const response = await apiClient.get('/orders');

      expect(response.status).toBe(200);
      expect(orderRequestCount).toBe(2);
      expect(useAuthStore.getState().accessToken).toBe('new-token');
    });

    it('retries original request after successful refresh', async () => {
      useAuthStore.getState().login('expired-token', mockUser);

      let orderRequestCount = 0;

      server.use(
        http.get(`${API_BASE}/orders`, () => {
          orderRequestCount++;
          if (orderRequestCount === 1) {
            return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
          }
          return HttpResponse.json({
            content: [{ id: 1, status: 'CONFIRMED' }],
            totalElements: 1,
          });
        }),
        http.post(`${API_BASE}/auth/refresh`, () => {
          return HttpResponse.json({ accessToken: 'refreshed-token' });
        }),
      );

      const response = await apiClient.get('/orders');

      expect(response.data.content).toHaveLength(1);
      expect(response.data.content[0].status).toBe('CONFIRMED');
    });

    it('logs out user on failed refresh', async () => {
      useAuthStore.getState().login('expired-token', mockUser);
      expect(useAuthStore.getState().isAuthenticated).toBe(true);

      server.use(
        http.get(`${API_BASE}/orders`, () => {
          return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
        }),
        http.post(`${API_BASE}/auth/refresh`, () => {
          return HttpResponse.json({ message: 'Refresh token expired' }, { status: 401 });
        }),
      );

      await expect(apiClient.get('/orders')).rejects.toThrow();

      expect(useAuthStore.getState().isAuthenticated).toBe(false);
      expect(useAuthStore.getState().accessToken).toBeNull();
      expect(useAuthStore.getState().user).toBeNull();
    });
  });
});
