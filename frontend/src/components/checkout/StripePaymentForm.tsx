import { useState } from 'react';
import { Elements, PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { formatCurrency } from '@/lib/formatters';

const stripePublishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;
const stripePromise = stripePublishableKey ? loadStripe(stripePublishableKey) : null;

interface StripePaymentFormProps {
  clientSecret: string;
  total: number;
  onSuccess: (paymentIntentId: string) => void;
  onError: (message: string) => void;
  isProcessing: boolean;
}

function CheckoutForm({
  total,
  onSuccess,
  onError,
  isProcessing,
}: Readonly<Omit<StripePaymentFormProps, 'clientSecret'>>) {
  const stripe = useStripe();
  const elements = useElements();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!stripe || !elements) {
      return;
    }

    setIsSubmitting(true);

    const result = await stripe.confirmPayment({
      elements,
      redirect: 'if_required',
    });

    if (result.error) {
      onError(result.error.message ?? 'Payment failed');
      setIsSubmitting(false);
    } else if (result.paymentIntent) {
      onSuccess(result.paymentIntent.id);
    }
  };

  const disabled = !stripe || !elements || isSubmitting || isProcessing;

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <PaymentElement />
      <Button type="submit" disabled={disabled} className="w-full">
        {isSubmitting || isProcessing ? (
          <>
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            Processing...
          </>
        ) : (
          `Pay ${formatCurrency(total)}`
        )}
      </Button>
    </form>
  );
}

interface StripeWrapperProps {
  clientSecret: string | null;
  total: number;
  onSuccess: (paymentIntentId: string) => void;
  onError: (message: string) => void;
  isProcessing: boolean;
}

export function StripePaymentForm({
  clientSecret,
  total,
  onSuccess,
  onError,
  isProcessing,
}: Readonly<StripeWrapperProps>) {
  if (!stripePromise) {
    return (
      <div className="rounded-lg border border-dashed p-6 text-center">
        <p className="text-sm text-muted-foreground">
          Stripe is not configured. Set VITE_STRIPE_PUBLISHABLE_KEY in your .env file.
        </p>
      </div>
    );
  }

  if (!clientSecret) {
    return (
      <div className="flex items-center justify-center p-6">
        <Loader2 className="mr-2 h-5 w-5 animate-spin text-muted-foreground" />
        <span className="text-sm text-muted-foreground">Preparing payment...</span>
      </div>
    );
  }

  return (
    <Elements stripe={stripePromise} options={{ clientSecret }}>
      <CheckoutForm
        total={total}
        onSuccess={onSuccess}
        onError={onError}
        isProcessing={isProcessing}
      />
    </Elements>
  );
}
