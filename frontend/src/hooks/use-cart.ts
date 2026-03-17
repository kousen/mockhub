import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as cartApi from '@/api/cart';
import { useAuthStore } from '@/stores/auth-store';
import { useCartStore } from '@/stores/cart-store';
import type { AddToCartRequest, Cart } from '@/types/cart';

/**
 * Hook for fetching the current user's cart.
 * Only enabled when the user is authenticated.
 * Syncs the cart item count to the Zustand cart store.
 */
export function useCart() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const setItemCount = useCartStore((state) => state.setItemCount);

  const query = useQuery({
    queryKey: ['cart'],
    queryFn: () => cartApi.getCart(),
    enabled: isAuthenticated,
  });

  useEffect(() => {
    if (query.data) {
      setItemCount(query.data.itemCount);
    }
  }, [query.data, setItemCount]);

  return query;
}

/**
 * Hook for adding an item to the cart.
 * Uses optimistic updates to increment the cart count immediately,
 * then invalidates the cart query on success.
 */
export function useAddToCart() {
  const queryClient = useQueryClient();
  const setItemCount = useCartStore((state) => state.setItemCount);

  return useMutation({
    mutationFn: (data: AddToCartRequest) => cartApi.addToCart(data),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['cart'] });
      const previousCart = queryClient.getQueryData<Cart>(['cart']);
      if (previousCart) {
        const optimisticCart: Cart = {
          ...previousCart,
          itemCount: previousCart.itemCount + 1,
        };
        queryClient.setQueryData(['cart'], optimisticCart);
        setItemCount(optimisticCart.itemCount);
      }
      return { previousCart };
    },
    onError: (_error, _variables, context) => {
      if (context?.previousCart) {
        queryClient.setQueryData(['cart'], context.previousCart);
        setItemCount(context.previousCart.itemCount);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
    },
  });
}

/**
 * Hook for removing an item from the cart.
 * Uses optimistic updates to remove the item from the cache immediately.
 */
export function useRemoveFromCart() {
  const queryClient = useQueryClient();
  const setItemCount = useCartStore((state) => state.setItemCount);

  return useMutation({
    mutationFn: (itemId: number) => cartApi.removeFromCart(itemId),
    onMutate: async (itemId) => {
      await queryClient.cancelQueries({ queryKey: ['cart'] });
      const previousCart = queryClient.getQueryData<Cart>(['cart']);
      if (previousCart) {
        const optimisticCart: Cart = {
          ...previousCart,
          items: previousCart.items.filter((item) => item.id !== itemId),
          itemCount: Math.max(0, previousCart.itemCount - 1),
          subtotal: previousCart.subtotal -
            (previousCart.items.find((item) => item.id === itemId)?.currentPrice ?? 0),
        };
        queryClient.setQueryData(['cart'], optimisticCart);
        setItemCount(optimisticCart.itemCount);
      }
      return { previousCart };
    },
    onError: (_error, _variables, context) => {
      if (context?.previousCart) {
        queryClient.setQueryData(['cart'], context.previousCart);
        setItemCount(context.previousCart.itemCount);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
    },
  });
}

/**
 * Hook for clearing the entire cart.
 */
export function useClearCart() {
  const queryClient = useQueryClient();
  const setItemCount = useCartStore((state) => state.setItemCount);

  return useMutation({
    mutationFn: () => cartApi.clearCart(),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['cart'] });
      const previousCart = queryClient.getQueryData<Cart>(['cart']);
      if (previousCart) {
        const emptyCart: Cart = {
          ...previousCart,
          items: [],
          itemCount: 0,
          subtotal: 0,
        };
        queryClient.setQueryData(['cart'], emptyCart);
        setItemCount(0);
      }
      return { previousCart };
    },
    onError: (_error, _variables, context) => {
      if (context?.previousCart) {
        queryClient.setQueryData(['cart'], context.previousCart);
        setItemCount(context.previousCart.itemCount);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['cart'] });
    },
  });
}
