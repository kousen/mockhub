import { Link } from 'react-router';
import { ShoppingCart } from 'lucide-react';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetFooter,
} from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { CartItem } from './CartItem';
import { useCart, useRemoveFromCart } from '@/hooks/use-cart';
import { useCartStore } from '@/stores/cart-store';
import { formatCurrency } from '@/lib/formatters';
import { ROUTES } from '@/lib/constants';

/**
 * Slide-out cart drawer from the right side.
 * Shows cart items, subtotal, and navigation buttons.
 * Rendered in MainLayout so it's accessible from any page.
 */
export function CartDrawer() {
  const isDrawerOpen = useCartStore((state) => state.isDrawerOpen);
  const closeDrawer = useCartStore((state) => state.closeDrawer);
  const { data: cart, isLoading } = useCart();
  const removeFromCart = useRemoveFromCart();

  const handleRemove = (itemId: number) => {
    removeFromCart.mutate(itemId);
  };

  return (
    <Sheet open={isDrawerOpen} onOpenChange={closeDrawer}>
      <SheetContent side="right" className="flex w-full flex-col sm:max-w-[400px]">
        <SheetHeader>
          <SheetTitle className="flex items-center gap-2">
            <ShoppingCart className="h-5 w-5" />
            Your Cart
          </SheetTitle>
        </SheetHeader>

        {isLoading ? (
          <div className="flex-1 space-y-3 p-4">
            {Array.from({ length: 3 }).map((_, index) => (
              <Skeleton key={index} className="h-20 w-full" />
            ))}
          </div>
        ) : !cart || cart.items.length === 0 ? (
          <div className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-center">
            <ShoppingCart className="h-12 w-12 text-muted-foreground/40" />
            <p className="text-sm text-muted-foreground">Your cart is empty</p>
            <Button variant="outline" onClick={closeDrawer} asChild>
              <Link to={ROUTES.EVENTS}>Browse Events</Link>
            </Button>
          </div>
        ) : (
          <>
            <div className="flex-1 overflow-y-auto space-y-2 px-4">
              {cart.items.map((item) => (
                <CartItem
                  key={item.id}
                  item={item}
                  onRemove={handleRemove}
                  isRemoving={removeFromCart.isPending}
                />
              ))}
            </div>

            <SheetFooter className="border-t">
              <div className="w-full space-y-3">
                <Separator />
                <div className="flex justify-between text-sm font-semibold">
                  <span>Subtotal ({cart.itemCount} items)</span>
                  <span>{formatCurrency(cart.subtotal)}</span>
                </div>
                <div className="flex flex-col gap-2">
                  <Button asChild onClick={closeDrawer}>
                    <Link to={ROUTES.CHECKOUT}>Checkout</Link>
                  </Button>
                  <Button variant="outline" asChild onClick={closeDrawer}>
                    <Link to={ROUTES.CART}>View Cart</Link>
                  </Button>
                </div>
              </div>
            </SheetFooter>
          </>
        )}
      </SheetContent>
    </Sheet>
  );
}
