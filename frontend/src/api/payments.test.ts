import { describe, it, expect, vi } from 'vitest';
import { createPaymentIntent, confirmPayment } from './payments';

vi.mock('./client', () => ({
  default: {
    post: vi.fn().mockResolvedValue({ data: 'mock' }),
  },
}));

describe('payments API', () => {
  it('createPaymentIntent posts order number', async () => {
    const client = (await import('./client')).default;
    await createPaymentIntent('ORD-001');
    expect(client.post).toHaveBeenCalledWith('/payments/create-intent', { orderNumber: 'ORD-001' });
  });

  it('confirmPayment posts payment intent ID', async () => {
    const client = (await import('./client')).default;
    await confirmPayment('pi_test');
    expect(client.post).toHaveBeenCalledWith('/payments/confirm', { paymentIntentId: 'pi_test' });
  });
});
