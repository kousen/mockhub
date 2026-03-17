import { create } from 'zustand';

interface CartState {
  itemCount: number;
  isDrawerOpen: boolean;
}

interface CartActions {
  setItemCount: (count: number) => void;
  openDrawer: () => void;
  closeDrawer: () => void;
  toggleDrawer: () => void;
}

/**
 * Cart UI store using Zustand.
 *
 * Tracks the cart item count (synced from React Query cart data)
 * and the open/closed state of the cart drawer.
 */
export const useCartStore = create<CartState & CartActions>()((set) => ({
  itemCount: 0,
  isDrawerOpen: false,

  setItemCount: (count: number) => set({ itemCount: count }),

  openDrawer: () => set({ isDrawerOpen: true }),

  closeDrawer: () => set({ isDrawerOpen: false }),

  toggleDrawer: () => set((state) => ({ isDrawerOpen: !state.isDrawerOpen })),
}));
