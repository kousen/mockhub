import { Link } from 'react-router';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { formatCurrency } from '@/lib/formatters';
import { ROUTES } from '@/lib/constants';

interface CartSummaryProps {
  subtotal: number;
  itemCount: number;
  showCheckoutButton?: boolean;
}

const SERVICE_FEE_RATE = 0.1;

/**
 * Cart summary showing subtotal, service fee (10%), and total.
 * Optionally shows a "Proceed to Checkout" button.
 */
export function CartSummary({ subtotal, itemCount, showCheckoutButton = true }: CartSummaryProps) {
  const serviceFee = subtotal * SERVICE_FEE_RATE;
  const total = subtotal + serviceFee;

  return (
    <div className="rounded-lg border p-4">
      <h3 className="text-sm font-semibold">
        Order Summary ({itemCount} {itemCount === 1 ? 'item' : 'items'})
      </h3>
      <Separator className="my-3" />
      <div className="space-y-2 text-sm">
        <div className="flex justify-between">
          <span className="text-muted-foreground">Subtotal</span>
          <span>{formatCurrency(subtotal)}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-muted-foreground">Service Fee (10%)</span>
          <span>{formatCurrency(serviceFee)}</span>
        </div>
        <Separator className="my-2" />
        <div className="flex justify-between font-semibold text-base">
          <span>Total</span>
          <span>{formatCurrency(total)}</span>
        </div>
      </div>
      {showCheckoutButton && (
        <Button className="mt-4 w-full" size="lg" asChild>
          <Link to={ROUTES.CHECKOUT}>Proceed to Checkout</Link>
        </Button>
      )}
    </div>
  );
}
