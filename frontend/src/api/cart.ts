import apiClient from './client';
import type { Cart, AddToCartRequest } from '@/types/cart';

export async function getCart(): Promise<Cart> {
  const response = await apiClient.get<Cart>('/cart');
  return response.data;
}

export async function addToCart(data: AddToCartRequest): Promise<Cart> {
  const response = await apiClient.post<Cart>('/cart/items', data);
  return response.data;
}

export async function removeFromCart(itemId: number): Promise<Cart> {
  const response = await apiClient.delete<Cart>(`/cart/items/${itemId}`);
  return response.data;
}

export async function clearCart(): Promise<void> {
  await apiClient.delete('/cart');
}
