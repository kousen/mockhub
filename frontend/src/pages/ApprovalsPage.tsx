import { useEffect, useState, useCallback } from 'react';
import { toast } from 'sonner';
import { BellRing, Bot, Check, Loader2, ShieldCheck, X } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { EmptyState } from '@/components/common/EmptyState';
import {
  isActionablePending,
  useApproveApproval,
  useDenyApproval,
  useMyApprovals,
} from '@/hooks/use-agent-approvals';
import type { AgentPurchaseApproval, AgentApprovalStatus } from '@/types/agentApproval';

function formatCurrency(amount: number): string {
  return `$${amount.toFixed(2)}`;
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString();
}

interface SnapshotItem {
  event?: string;
  section?: string;
  row?: string;
  seat?: string;
  quantity?: number;
  unitPrice?: number;
}

/**
 * proposedOrderSnapshot is agent-supplied JSON or free text — parse
 * defensively and fall back to showing the raw string.
 */
const MAX_SNAPSHOT_CHARS = 10_000;
const MAX_RENDERED_ITEMS = 10;
const MAX_FIELD_CHARS = 120;

export function parseSnapshot(snapshot: string | null): SnapshotItem[] | null {
  if (!snapshot) return null;
  // Agent-supplied and unbounded — refuse to parse pathological payloads
  if (snapshot.length > MAX_SNAPSHOT_CHARS) return null;
  try {
    const parsed: unknown = JSON.parse(snapshot);
    const rawItems: unknown[] = Array.isArray(parsed)
      ? parsed
      : Array.isArray((parsed as { items?: unknown[] }).items)
        ? ((parsed as { items: unknown[] }).items ?? [])
        : [parsed];
    const items = rawItems
      .filter((item): item is Record<string, unknown> => typeof item === 'object' && item !== null)
      .map((item) => ({
        event: str(item, ['eventName', 'event', 'name']),
        section: str(item, ['sectionName', 'section']),
        row: str(item, ['rowLabel', 'row']),
        seat: str(item, ['seatNumber', 'seat']),
        quantity: num(item, ['quantity', 'qty']),
        unitPrice: num(item, ['unitPrice', 'price', 'listedPrice', 'computedPrice']),
      }))
      .filter((item) => item.event ?? item.section ?? item.seat ?? item.unitPrice);
    return items.length > 0 ? items : null;
  } catch {
    return null;
  }
}

function str(obj: Record<string, unknown>, keys: string[]): string | undefined {
  for (const key of keys) {
    const value = obj[key];
    if (typeof value === 'string' && value) return value.slice(0, MAX_FIELD_CHARS);
    if (typeof value === 'number') return String(value);
  }
  return undefined;
}

function num(obj: Record<string, unknown>, keys: string[]): number | undefined {
  for (const key of keys) {
    const value = obj[key];
    if (typeof value === 'number') return value;
  }
  return undefined;
}

function OrderSnapshot({ snapshot }: Readonly<{ snapshot: string | null }>) {
  const items = parseSnapshot(snapshot);
  if (!snapshot) return null;
  if (!items) {
    return (
      <p className="break-words rounded-md bg-muted p-2 text-xs text-muted-foreground">
        {snapshot.length > 300 ? `${snapshot.slice(0, 300)}…` : snapshot}
      </p>
    );
  }
  const shown = items.slice(0, MAX_RENDERED_ITEMS);
  return (
    <ul className="space-y-1 text-sm">
      {shown.map((item, index) => (
        <li key={`${item.event}-${item.section}-${item.seat}-${index}`}>
          {item.event && <span className="font-medium">{item.event}</span>}
          <span className="text-muted-foreground">
            {item.section && <> · {item.section}</>}
            {item.row && <> · Row {item.row}</>}
            {item.seat && <> · Seat {item.seat}</>}
            {item.quantity !== undefined && item.quantity > 1 && <> · ×{item.quantity}</>}
            {item.unitPrice !== undefined && <> · {formatCurrency(item.unitPrice)}</>}
          </span>
        </li>
      ))}
      {items.length > MAX_RENDERED_ITEMS && (
        <li className="text-xs text-muted-foreground">
          …and {items.length - MAX_RENDERED_ITEMS} more items
        </li>
      )}
    </ul>
  );
}

/** Live countdown to an expiry timestamp; reports expiry to the parent. */
function ExpiryCountdown({
  expiresAt,
  onExpired,
}: Readonly<{ expiresAt: string; onExpired: (expired: boolean) => void }>) {
  const [remainingMs, setRemainingMs] = useState(() => Date.parse(expiresAt) - Date.now());

  useEffect(() => {
    const tick = () => {
      const ms = Date.parse(expiresAt) - Date.now();
      setRemainingMs(ms);
      onExpired(ms <= 0);
    };
    tick();
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
  }, [expiresAt, onExpired]);

  if (remainingMs <= 0) {
    return <Badge variant="secondary">Expired</Badge>;
  }
  const totalSeconds = Math.floor(remainingMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  const display =
    minutes >= 60 ? `${Math.floor(minutes / 60)}h ${minutes % 60}m` : `${minutes}m ${seconds}s`;
  return (
    <span className="text-sm font-medium text-amber-600 dark:text-amber-400">
      Expires in {display}
    </span>
  );
}

function PendingApprovalCard({ approval }: Readonly<{ approval: AgentPurchaseApproval }>) {
  const approve = useApproveApproval();
  const deny = useDenyApproval();
  const [showDenyForm, setShowDenyForm] = useState(false);
  const [denyReason, setDenyReason] = useState('');
  const [expired, setExpired] = useState(false);

  const inFlight = approve.isPending || deny.isPending;
  const disabled = inFlight || expired;

  const handleApprove = useCallback(() => {
    approve.mutate(approval.approvalId, {
      onSuccess: () => toast.success('Purchase approved.'),
      onError: () => toast.error('Failed to approve purchase.'),
    });
  }, [approve, approval.approvalId]);

  const handleDeny = useCallback(() => {
    deny.mutate(
      { approvalId: approval.approvalId, reason: denyReason.trim() || undefined },
      {
        onSuccess: () => toast.success('Purchase denied.'),
        onError: () => toast.error('Failed to deny purchase.'),
      },
    );
  }, [deny, approval.approvalId, denyReason]);

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between gap-2">
          <CardTitle className="flex items-center gap-2 text-base">
            <Bot className="h-5 w-5 shrink-0" />
            <span className="break-all">{approval.agentId}</span>
          </CardTitle>
          {approval.expiresAt && (
            <ExpiryCountdown expiresAt={approval.expiresAt} onExpired={setExpired} />
          )}
        </div>
        <p className="text-xs text-muted-foreground">
          Mandate {approval.mandateId} · Proposed {formatDateTime(approval.proposedAt)}
        </p>
      </CardHeader>
      <CardContent className="space-y-3">
        <OrderSnapshot snapshot={approval.proposedOrderSnapshot} />

        {approval.agentRationale && (
          <p className="text-sm italic text-muted-foreground">
            &ldquo;{approval.agentRationale.slice(0, 500)}&rdquo;
          </p>
        )}

        {approval.commercePolicySnapshot && (
          <details className="text-xs text-muted-foreground">
            <summary className="cursor-pointer font-medium">Policy at proposal time</summary>
            <p className="mt-1 break-words">{approval.commercePolicySnapshot.slice(0, 600)}</p>
          </details>
        )}

        <Separator />

        <div className="space-y-1 text-sm">
          <div className="flex justify-between text-muted-foreground">
            <span>Subtotal</span>
            <span>{formatCurrency(approval.subtotal)}</span>
          </div>
          <div className="flex justify-between text-muted-foreground">
            <span>Service fee</span>
            <span>{formatCurrency(approval.serviceFee)}</span>
          </div>
          <div className="flex justify-between text-xl font-bold">
            <span>Total</span>
            <span>{formatCurrency(approval.total)}</span>
          </div>
        </div>

        {showDenyForm ? (
          <div className="space-y-2">
            <Input
              value={denyReason}
              onChange={(e) => setDenyReason(e.target.value)}
              placeholder="Reason (optional)"
              aria-label="Denial reason"
            />
            <div className="flex gap-2">
              <Button
                variant="destructive"
                className="h-12 flex-1"
                onClick={handleDeny}
                disabled={disabled}
              >
                {deny.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <>
                    <X className="mr-2 h-4 w-4" />
                    Confirm Deny
                  </>
                )}
              </Button>
              <Button
                variant="ghost"
                className="h-12"
                onClick={() => setShowDenyForm(false)}
                disabled={inFlight}
              >
                Cancel
              </Button>
            </div>
          </div>
        ) : (
          <div className="flex gap-2">
            <Button className="h-12 flex-1" onClick={handleApprove} disabled={disabled}>
              {approve.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <>
                  <Check className="mr-2 h-4 w-4" />
                  Approve
                </>
              )}
            </Button>
            <Button
              variant="outline"
              className="h-12 flex-1"
              onClick={() => setShowDenyForm(true)}
              disabled={disabled}
            >
              <X className="mr-2 h-4 w-4" />
              Deny
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function statusBadgeVariant(
  status: AgentApprovalStatus,
): 'default' | 'secondary' | 'destructive' | 'outline' {
  switch (status) {
    case 'COMPLETED':
      return 'default';
    case 'APPROVED':
      return 'outline';
    case 'DENIED':
    case 'FAILED':
      return 'destructive';
    default:
      return 'secondary';
  }
}

function decisionTimestamp(approval: AgentPurchaseApproval): string | null {
  const ts =
    approval.completedAt ??
    approval.failedAt ??
    approval.deniedAt ??
    approval.approvedAt ??
    approval.expiresAt;
  return ts ? formatDateTime(ts) : null;
}

function HistoryRow({ approval }: Readonly<{ approval: AgentPurchaseApproval }>) {
  // A PROPOSED record only reaches history when its expiry passed but the
  // cleanup job hasn't persisted EXPIRED yet — show what it effectively is.
  const displayStatus = approval.status === 'PROPOSED' ? 'EXPIRED' : approval.status;
  return (
    <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg border p-3 text-sm">
      <div className="min-w-0">
        <p className="font-medium">
          {approval.agentId} · {formatCurrency(approval.total)}
        </p>
        <p className="text-xs text-muted-foreground">
          {decisionTimestamp(approval)}
          {approval.finalOrderNumber && <> · Order {approval.finalOrderNumber}</>}
        </p>
        {approval.denialReason && (
          <p className="text-xs text-muted-foreground">Denied: {approval.denialReason}</p>
        )}
        {approval.failureReason && (
          <p className="text-xs text-muted-foreground">Failed: {approval.failureReason}</p>
        )}
      </div>
      <Badge variant={statusBadgeVariant(displayStatus)}>{displayStatus}</Badge>
    </div>
  );
}

/**
 * Human-facing approval checkpoint for agent purchase proposals. This page is
 * deliberately the ONLY approval surface — the MCP approve/deny tools were
 * removed because an agent could approve its own proposal in-band.
 */
export function ApprovalsPage() {
  const { data: approvals, isLoading, isError, refetch } = useMyApprovals();

  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-6 sm:px-6">
        <Skeleton className="mb-6 h-8 w-64" />
        <div className="space-y-3">
          {Array.from({ length: 3 }, (_, i) => i).map((n) => (
            <Skeleton key={`skeleton-${n}`} className="h-48 w-full" />
          ))}
        </div>
      </div>
    );
  }

  const all = approvals ?? [];
  const pending = all
    .filter(isActionablePending)
    .sort((a, b) => b.proposedAt.localeCompare(a.proposedAt));
  const history = all
    .filter((a) => !isActionablePending(a))
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt));

  return (
    <div className="mx-auto max-w-2xl px-4 py-6 sm:px-6">
      <h1 className="mb-6 flex items-center gap-2 text-2xl font-bold">
        <ShieldCheck className="h-6 w-6" />
        Purchase Approvals
      </h1>

      {isError && (
        <div className="mb-4 flex items-center justify-between rounded-lg border border-destructive/50 bg-destructive/10 p-3 text-sm">
          <span>Couldn&apos;t load approvals — pending proposals may be hidden.</span>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Retry
          </Button>
        </div>
      )}

      <section aria-label="Pending approvals">
        {pending.length === 0 ? (
          <EmptyState
            icon={BellRing}
            title="Nothing waiting for you"
            description="When an agent proposes a purchase that needs your approval, it will appear here."
          />
        ) : (
          <div className="space-y-4">
            {pending.map((approval) => (
              <PendingApprovalCard key={approval.approvalId} approval={approval} />
            ))}
          </div>
        )}
      </section>

      {history.length > 0 && (
        <section aria-label="Approval history" className="mt-8">
          <h2 className="mb-3 text-lg font-semibold">History</h2>
          <div className="space-y-2">
            {history.map((approval) => (
              <HistoryRow key={approval.approvalId} approval={approval} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
