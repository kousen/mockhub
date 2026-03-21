import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router';
import { CheckCircle, XCircle, AlertTriangle, Loader2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import apiClient from '@/api/client';

interface VerificationResult {
  valid: boolean;
  orderNumber: string | null;
  ticketId: number | null;
  eventSlug: string | null;
  sectionName: string | null;
  rowLabel: string | null;
  seatNumber: string | null;
  alreadyScanned: boolean;
  scannedAt: string | null;
  message: string;
}

/**
 * Public ticket verification page. Accessed by scanning the QR code
 * on a ticket PDF. Displays verification result with ticket details.
 */
export function TicketVerifyPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [result, setResult] = useState<VerificationResult | null>(null);
  const [loading, setLoading] = useState(!!token);
  const [error, setError] = useState<string | null>(token ? null : 'No ticket token provided');

  useEffect(() => {
    if (!token) return;

    apiClient
      .get<VerificationResult>('/tickets/verify', { params: { token } })
      .then((response) => {
        setResult(response.data);
      })
      .catch(() => {
        setError('Failed to verify ticket. Please try again.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [token]);

  if (loading) {
    return (
      <div className="flex min-h-[calc(100vh-10rem)] items-center justify-center">
        <div className="text-center">
          <Loader2 className="mx-auto h-12 w-12 animate-spin text-muted-foreground" />
          <p className="mt-4 text-muted-foreground">Verifying ticket...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-[calc(100vh-10rem)] items-center justify-center">
        <div className="text-center">
          <XCircle className="mx-auto h-16 w-16 text-red-500" />
          <h1 className="mt-4 text-2xl font-bold">Verification Error</h1>
          <p className="mt-2 text-muted-foreground">{error}</p>
        </div>
      </div>
    );
  }

  if (!result) return null;

  return (
    <div className="mx-auto max-w-md px-4 py-8 sm:px-6">
      {/* Status Icon */}
      <div className="text-center">
        {result.valid && !result.alreadyScanned && (
          <CheckCircle className="mx-auto h-20 w-20 text-emerald-500" />
        )}
        {result.valid && result.alreadyScanned && (
          <AlertTriangle className="mx-auto h-20 w-20 text-amber-500" />
        )}
        {!result.valid && <XCircle className="mx-auto h-20 w-20 text-red-500" />}

        <h1 className="mt-4 text-2xl font-bold">
          {result.valid && !result.alreadyScanned && 'Ticket Valid'}
          {result.valid && result.alreadyScanned && 'Already Scanned'}
          {!result.valid && 'Invalid Ticket'}
        </h1>

        <Badge
          variant="secondary"
          className={
            result.valid && !result.alreadyScanned
              ? 'mt-2 bg-emerald-100 text-emerald-800'
              : result.valid && result.alreadyScanned
                ? 'mt-2 bg-amber-100 text-amber-800'
                : 'mt-2 bg-red-100 text-red-800'
          }
        >
          {result.message}
        </Badge>
      </div>

      {/* Ticket Details (only for valid tickets) */}
      {result.valid && (
        <div className="mt-8 space-y-3 rounded-lg border p-4">
          <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
            Ticket Details
          </h2>

          {result.orderNumber && (
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Order</span>
              <span className="font-mono font-medium">{result.orderNumber}</span>
            </div>
          )}

          {result.sectionName && (
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Section</span>
              <span className="font-medium">{result.sectionName}</span>
            </div>
          )}

          {result.rowLabel && (
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Row</span>
              <span className="font-medium">{result.rowLabel}</span>
            </div>
          )}

          {result.seatNumber && (
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Seat</span>
              <span className="font-medium">{result.seatNumber}</span>
            </div>
          )}

          {result.alreadyScanned && result.scannedAt && (
            <div className="mt-2 rounded bg-amber-50 p-2 text-xs text-amber-800">
              First scanned at {new Date(result.scannedAt).toLocaleString()}
            </div>
          )}
        </div>
      )}

      {/* Actions */}
      <div className="mt-8 text-center">
        <Button variant="outline" asChild>
          <Link to="/">Back to MockHub</Link>
        </Button>
      </div>
    </div>
  );
}
