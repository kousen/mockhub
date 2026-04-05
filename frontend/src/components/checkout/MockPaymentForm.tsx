import { CreditCard, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { formatCurrency } from '@/lib/formatters';

interface MockPaymentFormProps {
  total: number;
  onSubmit: () => void;
  isProcessing: boolean;
}

/**
 * Mock payment form for the testing/demo flow.
 * Provides a simple "Pay Now" button that triggers the mock checkout.
 */
export function MockPaymentForm({ total, onSubmit, isProcessing }: Readonly<MockPaymentFormProps>) {
  return (
    <div className="space-y-4 rounded-lg border p-6">
      <div className="flex items-center gap-2">
        <CreditCard className="h-5 w-5 text-muted-foreground" />
        <h3 className="font-semibold">Mock Payment</h3>
      </div>
      <p className="text-sm text-muted-foreground">
        This is a test payment. No real charges will be made.
      </p>
      <div className="rounded-md bg-muted p-4 text-center">
        <p className="text-xs text-muted-foreground">Amount to pay</p>
        <p className="text-2xl font-bold">{formatCurrency(total)}</p>
      </div>
      <Button className="w-full" size="lg" onClick={onSubmit} disabled={isProcessing}>
        {isProcessing ? (
          <>
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            Processing...
          </>
        ) : (
          `Pay ${formatCurrency(total)}`
        )}
      </Button>
    </div>
  );
}
