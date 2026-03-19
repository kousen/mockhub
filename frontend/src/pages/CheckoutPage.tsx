import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { ShoppingCart } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Skeleton } from '@/components/ui/skeleton';
import { OrderReview } from '@/components/checkout/OrderReview';
import { MockPaymentForm } from '@/components/checkout/MockPaymentForm';
import { StripePaymentForm } from '@/components/checkout/StripePaymentForm';
import { EmptyState } from '@/components/common/EmptyState';
import { useCart } from '@/hooks/use-cart';
import { useCheckout } from '@/hooks/use-orders';
import { createPaymentIntent, confirmPayment } from '@/api/payments';
import { ROUTES } from '@/lib/constants';

const SERVICE_FEE_RATE = 0.1;

/**
 * Checkout page with order review and payment method selection.
 * Supports mock payment (instant) and Stripe (real card processing).
 */
export function CheckoutPage() {
  const { data: cart, isLoading } = useCart();
  const checkoutMutation = useCheckout();
  const navigate = useNavigate();
  const [paymentTab, setPaymentTab] = useState<string>('mock');
  const [clientSecret, setClientSecret] = useState<string | null>(null);
  const [isCreatingIntent, setIsCreatingIntent] = useState(false);

  const handleMockCheckout = useCallback(() => {
    checkoutMutation.mutate(
      { paymentMethod: 'MOCK' },
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
  }, [checkoutMutation, navigate]);

  const handleStripeSuccess = useCallback(
    async (paymentIntentId: string) => {
      try {
        const confirmation = await confirmPayment(paymentIntentId);
        toast.success('Payment successful!');
        navigate(`/orders/${confirmation.orderNumber}/confirmation`);
      } catch {
        toast.error('Payment confirmation failed. Please check your orders.');
        navigate(ROUTES.ORDERS);
      }
    },
    [navigate],
  );

  const handleStripeError = useCallback((message: string) => {
    toast.error(message);
  }, []);

  // Create a Stripe payment intent when the user selects the Stripe tab
  useEffect(() => {
    if (paymentTab !== 'stripe' || clientSecret || isCreatingIntent) {
      return;
    }

    const createIntent = async () => {
      setIsCreatingIntent(true);
      try {
        // First create the order, then create a payment intent for it
        const order = await new Promise<{ orderNumber: string }>((resolve, reject) => {
          checkoutMutation.mutate(
            { paymentMethod: 'STRIPE' },
            { onSuccess: resolve, onError: reject },
          );
        });
        const intent = await createPaymentIntent(order.orderNumber);
        setClientSecret(intent.clientSecret);
      } catch {
        toast.error('Failed to set up payment. Please try again.');
        setPaymentTab('mock');
      } finally {
        setIsCreatingIntent(false);
      }
    };

    createIntent();
  }, [paymentTab, clientSecret, isCreatingIntent, checkoutMutation]);

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
        <EmptyState
          icon={ShoppingCart}
          title="Nothing to checkout"
          description="Add tickets to your cart first."
          action={{ label: 'Browse Events', href: ROUTES.EVENTS }}
        />
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
                onSubmit={handleMockCheckout}
                isProcessing={checkoutMutation.isPending}
              />
            </TabsContent>
            <TabsContent value="stripe">
              <StripePaymentForm
                clientSecret={clientSecret}
                total={total}
                onSuccess={handleStripeSuccess}
                onError={handleStripeError}
                isProcessing={isCreatingIntent}
              />
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}
