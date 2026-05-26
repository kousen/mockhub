import { Link, useNavigate } from 'react-router';
import { ArrowLeft, ShoppingCart } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { OrderReview } from '@/components/checkout/OrderReview';
import { EmptyState } from '@/components/common/EmptyState';
import { useCart } from '@/hooks/use-cart';
import { markReviewPassed } from '@/lib/checkout-review-gate';
import { ROUTES } from '@/lib/constants';

/**
 * Dedicated review step shown between cart and payment.
 * Renders the existing OrderReview (itemized breakdown + CartSummary's
 * fee math) and gates entry to /checkout via sessionStorage fingerprint.
 */
export function OrderReviewPage() {
  const { data: cart, isLoading } = useCart();
  const navigate = useNavigate();

  const handleContinue = () => {
    if (!cart) return;
    markReviewPassed(cart);
    navigate(ROUTES.CHECKOUT);
  };

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
        <Skeleton className="mb-6 h-8 w-48" />
        <div className="space-y-3">
          {Array.from({ length: 3 }, (_, i) => i).map((n) => (
            <Skeleton key={`skeleton-${n}`} className="h-24 w-full" />
          ))}
        </div>
        <Skeleton className="mt-6 h-48 w-full" />
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
        <EmptyState
          icon={ShoppingCart}
          title="Nothing to review"
          description="Add tickets to your cart before reviewing your order."
          action={{ label: 'Browse Events', href: ROUTES.EVENTS }}
        />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-6 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center gap-3">
        <Button variant="ghost" size="icon" asChild>
          <Link to={ROUTES.CART} aria-label="Back to cart">
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <h1 className="text-2xl font-bold">Order Review</h1>
      </div>

      <OrderReview cart={cart} />

      <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
        <Button variant="outline" asChild>
          <Link to={ROUTES.CART}>Back to Cart</Link>
        </Button>
        <Button size="lg" onClick={handleContinue}>
          Continue to Payment
        </Button>
      </div>
    </div>
  );
}
