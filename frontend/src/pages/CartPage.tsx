import { Link } from 'react-router';
import { ArrowLeft, ShoppingCart, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { CartItem } from '@/components/cart/CartItem';
import { CartSummary } from '@/components/cart/CartSummary';
import { useCart, useRemoveFromCart, useClearCart } from '@/hooks/use-cart';
import { ROUTES } from '@/lib/constants';

/**
 * Full-page cart view. Preferred on mobile instead of the drawer.
 * Shows all cart items with remove buttons and the order summary.
 */
export function CartPage() {
  const { data: cart, isLoading } = useCart();
  const removeFromCart = useRemoveFromCart();
  const clearCart = useClearCart();

  const handleRemove = (itemId: number) => {
    removeFromCart.mutate(itemId);
  };

  const handleClear = () => {
    clearCart.mutate();
  };

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-6 sm:px-6 lg:px-8">
        <Skeleton className="mb-6 h-8 w-48" />
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-24 w-full" />
          ))}
        </div>
        <Skeleton className="mt-6 h-48 w-full" />
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="flex min-h-[calc(100vh-16rem)] flex-col items-center justify-center gap-4 text-center">
          <ShoppingCart className="h-16 w-16 text-muted-foreground/30" />
          <h1 className="text-2xl font-bold">Your Cart is Empty</h1>
          <p className="text-muted-foreground">
            Find tickets for your favorite events and add them here.
          </p>
          <Button asChild>
            <Link to={ROUTES.EVENTS}>Browse Events</Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-6 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" asChild>
            <Link to={ROUTES.EVENTS}>
              <ArrowLeft className="h-4 w-4" />
              <span className="sr-only">Back to events</span>
            </Link>
          </Button>
          <h1 className="text-2xl font-bold">Shopping Cart</h1>
        </div>
        <Button
          variant="ghost"
          size="sm"
          className="text-destructive hover:text-destructive"
          onClick={handleClear}
          disabled={clearCart.isPending}
        >
          <Trash2 className="mr-1.5 h-3.5 w-3.5" />
          Clear Cart
        </Button>
      </div>

      {/* Cart Content */}
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-2 lg:col-span-2">
          {cart.items.map((item) => (
            <CartItem
              key={item.id}
              item={item}
              onRemove={handleRemove}
              isRemoving={removeFromCart.isPending}
            />
          ))}
        </div>
        <div>
          <CartSummary subtotal={cart.subtotal} itemCount={cart.itemCount} />
          <Button
            variant="link"
            className="mt-3 w-full text-sm"
            asChild
          >
            <Link to={ROUTES.EVENTS}>Continue Shopping</Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
