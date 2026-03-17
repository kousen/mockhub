import { useState } from 'react';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { ShoppingCart } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Skeleton } from '@/components/ui/skeleton';
import { OrderReview } from '@/components/checkout/OrderReview';
import { MockPaymentForm } from '@/components/checkout/MockPaymentForm';
import { StripePaymentForm } from '@/components/checkout/StripePaymentForm';
import { useCart } from '@/hooks/use-cart';
import { useCheckout } from '@/hooks/use-orders';
import { ROUTES } from '@/lib/constants';
import type { CheckoutRequest } from '@/types/order';
import { Link } from 'react-router';

const SERVICE_FEE_RATE = 0.1;

/**
 * Checkout page with order review and payment method selection.
 * Supports mock payment (for testing) and a Stripe placeholder.
 */
export function CheckoutPage() {
  const { data: cart, isLoading } = useCart();
  const checkout = useCheckout();
  const navigate = useNavigate();
  const [paymentTab, setPaymentTab] = useState<string>('mock');

  const handleCheckout = () => {
    const paymentMethod: CheckoutRequest['paymentMethod'] =
      paymentTab === 'stripe' ? 'STRIPE' : 'MOCK';

    checkout.mutate(
      { paymentMethod },
      {
        onSuccess: (order) => {
          toast.success('Payment successful!');
          navigate(`/orders/${order.orderNumber}/confirmation`);
        },
        onError: () => {
          toast.error('Payment failed. Please try again.');
        },
      },
    );
  };

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-6 sm:px-6 lg:px-8">
        <Skeleton className="mb-6 h-8 w-48" />
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, index) => (
              <Skeleton key={index} className="h-24 w-full" />
            ))}
          </div>
          <Skeleton className="h-64 w-full" />
        </div>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="flex min-h-[calc(100vh-16rem)] flex-col items-center justify-center gap-4 text-center">
          <ShoppingCart className="h-16 w-16 text-muted-foreground/30" />
          <h1 className="text-2xl font-bold">Nothing to Checkout</h1>
          <p className="text-muted-foreground">Add tickets to your cart first.</p>
          <Button asChild>
            <Link to={ROUTES.EVENTS}>Browse Events</Link>
          </Button>
        </div>
      </div>
    );
  }

  const total = cart.subtotal + cart.subtotal * SERVICE_FEE_RATE;

  return (
    <div className="mx-auto max-w-4xl px-4 py-6 sm:px-6 lg:px-8">
      <h1 className="mb-6 text-2xl font-bold">Checkout</h1>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Order Review */}
        <OrderReview cart={cart} />

        {/* Payment Method */}
        <div className="space-y-4">
          <h2 className="text-lg font-semibold">Payment Method</h2>
          <Tabs value={paymentTab} onValueChange={setPaymentTab}>
            <TabsList className="w-full">
              <TabsTrigger value="mock" className="flex-1">
                Mock Payment
              </TabsTrigger>
              <TabsTrigger value="stripe" className="flex-1">
                Stripe
              </TabsTrigger>
            </TabsList>
            <TabsContent value="mock">
              <MockPaymentForm
                total={total}
                onSubmit={handleCheckout}
                isProcessing={checkout.isPending}
              />
            </TabsContent>
            <TabsContent value="stripe">
              <StripePaymentForm />
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}
