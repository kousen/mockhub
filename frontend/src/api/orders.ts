import apiClient from './client';
import type { PageResponse } from '@/types/common';
import type { Order, OrderSummary, CheckoutRequest } from '@/types/order';

export async function checkout(data: CheckoutRequest): Promise<Order> {
  const response = await apiClient.post<Order>('/orders/checkout', data);
  return response.data;
}

export async function getOrders(page?: number, size?: number): Promise<PageResponse<OrderSummary>> {
  const response = await apiClient.get<PageResponse<OrderSummary>>('/orders', {
    params: { page, size },
  });
  return response.data;
}

export async function getOrder(orderNumber: string): Promise<Order> {
  const response = await apiClient.get<Order>(`/orders/${orderNumber}`);
  return response.data;
}

export async function downloadTicket(orderNumber: string, ticketId: number): Promise<Blob> {
  const response = await apiClient.get(`/orders/${orderNumber}/tickets/${ticketId}/download`, {
    responseType: 'blob',
  });
  return response.data as Blob;
}

export async function downloadCalendar(orderNumber: string): Promise<Blob> {
  const response = await apiClient.get(`/orders/${orderNumber}/calendar`, {
    responseType: 'blob',
  });
  return response.data as Blob;
}
