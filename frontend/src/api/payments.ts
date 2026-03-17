import apiClient from './client';
import type { PaymentIntent, PaymentConfirmation } from '@/types/order';

export async function createPaymentIntent(
  orderNumber: string,
): Promise<PaymentIntent> {
  const response = await apiClient.post<PaymentIntent>(
    `/payments/intent/${orderNumber}`,
  );
  return response.data;
}

export async function confirmPayment(
  paymentIntentId: string,
): Promise<PaymentConfirmation> {
  const response = await apiClient.post<PaymentConfirmation>(
    `/payments/confirm/${paymentIntentId}`,
  );
  return response.data;
}
