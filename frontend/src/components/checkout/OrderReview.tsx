import { CartItem } from '@/components/cart/CartItem';
import { CartSummary } from '@/components/cart/CartSummary';
import type { Cart } from '@/types/cart';

interface OrderReviewProps {
  cart: Cart;
}

/**
 * Read-only review of cart items for the checkout page.
 * Shows all items without remove buttons, plus the order summary.
 */
export function OrderReview({ cart }: OrderReviewProps) {
  return (
    <div className="space-y-4">
      <h2 className="text-lg font-semibold">Review Your Order</h2>
      <div className="space-y-2">
        {cart.items.map((item) => (
          <CartItem key={item.id} item={item} onRemove={() => {}} readOnly />
        ))}
      </div>
      <CartSummary subtotal={cart.subtotal} itemCount={cart.itemCount} showCheckoutButton={false} />
    </div>
  );
}
