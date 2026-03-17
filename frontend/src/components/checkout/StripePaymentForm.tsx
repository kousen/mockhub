import { CreditCard } from 'lucide-react';

/**
 * Placeholder for Stripe Elements integration.
 * Will be implemented when Stripe is fully configured.
 */
export function StripePaymentForm() {
  return (
    <div className="space-y-4 rounded-lg border border-dashed p-6 text-center">
      <CreditCard className="mx-auto h-10 w-10 text-muted-foreground/40" />
      <div>
        <h3 className="font-semibold">Stripe Payment</h3>
        <p className="mt-1 text-sm text-muted-foreground">
          Stripe payment integration coming soon.
        </p>
        <p className="mt-1 text-sm text-muted-foreground">
          Use the Mock Payment option for testing.
        </p>
      </div>
    </div>
  );
}
