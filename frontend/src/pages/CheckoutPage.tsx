import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { Loader2, ShoppingCart } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
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
 *
 * Mock flow: single click creates order + confirms immediately.
 * Stripe flow: user clicks "Pay with Stripe" → creates order → creates
 * payment intent → shows Stripe Elements → user enters card → confirms.
 */
export function CheckoutPage() {
  const { data: cart, isLoading } = useCart();
  const checkoutMutation = useCheckout();
  const navigate = useNavigate();
  const [paymentTab, setPaymentTab] = useState<string>('mock');

  // Stripe flow state
  const [clientSecret, setClientSecret] = useState<string | null>(null);
  const [stripeOrderTotal, setStripeOrderTotal] = useState<number | null>(null);
  const [isSettingUpStripe, setIsSettingUpStripe] = useState(false);
  const [stripeReady, setStripeReady] = useState(false);

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

  const handleStartStripe = useCallback(async () => {
    setIsSettingUpStripe(true);
    try {
      // Step 1: Create the order (this clears the cart)
      const order = await new Promise<{ orderNumber: string; total: number }>((resolve, reject) => {
        checkoutMutation.mutate(
          { paymentMethod: 'STRIPE' },
          {
            onSuccess: (o) => resolve({ orderNumber: o.orderNumber, total: o.total }),
            onError: reject,
          },
        );
      });
      setStripeOrderTotal(order.total);

      // Step 2: Create a payment intent
      const intent = await createPaymentIntent(order.orderNumber);
      setClientSecret(intent.clientSecret);
      setStripeReady(true);
    } catch {
      toast.error('Failed to set up Stripe payment. Please try again.');
    } finally {
      setIsSettingUpStripe(false);
    }
  }, [checkoutMutation]);

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

  // Show empty state only if cart is empty AND we're not in the Stripe flow
  if (!stripeReady && (!cart || cart.items.length === 0)) {
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

  const total = stripeOrderTotal ?? (cart ? cart.subtotal + cart.subtotal * SERVICE_FEE_RATE : 0);

  return (
    <div className="mx-auto max-w-4xl px-4 py-6 sm:px-6 lg:px-8">
      <h1 className="mb-6 text-2xl font-bold">Checkout</h1>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Order Review */}
        {cart && cart.items.length > 0 ? (
          <OrderReview cart={cart} />
        ) : (
          <div className="rounded-lg border p-6">
            <h3 className="font-semibold">Order Summary</h3>
            <p className="mt-2 text-sm text-muted-foreground">
              Your order has been created. Complete payment below.
            </p>
            <p className="mt-3 text-lg font-semibold">Total: ${total.toFixed(2)}</p>
          </div>
        )}

        {/* Payment Method */}
        <div className="space-y-4">
          <h2 className="text-lg font-semibold">Payment Method</h2>
          <Tabs value={paymentTab} onValueChange={setPaymentTab}>
            <TabsList className="w-full">
              <TabsTrigger value="mock" className="flex-1" disabled={stripeReady}>
                Mock Payment
              </TabsTrigger>
              <TabsTrigger value="stripe" className="flex-1" disabled={stripeReady}>
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
              {stripeReady ? (
                <StripePaymentForm
                  clientSecret={clientSecret}
                  total={total}
                  onSuccess={handleStripeSuccess}
                  onError={handleStripeError}
                  isProcessing={false}
                />
              ) : (
                <div className="space-y-4 rounded-lg border p-6">
                  <p className="text-sm text-muted-foreground">
                    Pay securely with your credit or debit card via Stripe. Use test card{' '}
                    <code className="text-xs">4242 4242 4242 4242</code> with any future expiry and
                    any CVC.
                  </p>
                  <Button
                    onClick={handleStartStripe}
                    disabled={isSettingUpStripe}
                    className="w-full"
                  >
                    {isSettingUpStripe ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        Setting up payment...
                      </>
                    ) : (
                      `Pay $${total.toFixed(2)} with Stripe`
                    )}
                  </Button>
                </div>
              )}
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </div>
  );
}
