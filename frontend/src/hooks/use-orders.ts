import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as ordersApi from '@/api/orders';
import type { CheckoutRequest } from '@/types/order';

/**
 * Hook for the checkout mutation.
 * Invalidates both cart and orders queries on success.
 */
export function useCheckout() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CheckoutRequest) => ordersApi.checkout(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
    },
  });
}

/**
 * Hook for fetching a paginated list of the current user's orders.
 */
export function useOrders(page?: number) {
  return useQuery({
    queryKey: ['orders', { page }],
    queryFn: () => ordersApi.getOrders(page),
  });
}

/**
 * Hook for fetching a single order by its order number.
 */
export function useOrder(orderNumber: string) {
  return useQuery({
    queryKey: ['orders', orderNumber],
    queryFn: () => ordersApi.getOrder(orderNumber),
    enabled: orderNumber.length > 0,
  });
}
