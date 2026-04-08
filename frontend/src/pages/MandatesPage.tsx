import { useState, useCallback } from 'react';
import { toast } from 'sonner';
import { Shield, Plus, Loader2, Calendar, Ban, Clock } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { EmptyState } from '@/components/common/EmptyState';
import { useMyMandates, useCreateMandate, useRevokeMandate } from '@/hooks/use-mandates';
import type { Mandate, CreateMandateRequest } from '@/types/mandate';

type StatusFilter = 'ACTIVE' | 'EXPIRED' | 'REVOKED';

function filterMandates(mandates: Mandate[], filter: StatusFilter): Mandate[] {
  const now = new Date().toISOString();
  switch (filter) {
    case 'ACTIVE':
      return mandates.filter(
        (m) => m.status === 'ACTIVE' && (m.expiresAt === null || m.expiresAt > now),
      );
    case 'EXPIRED':
      return mandates.filter(
        (m) => m.status === 'ACTIVE' && m.expiresAt !== null && m.expiresAt <= now,
      );
    case 'REVOKED':
      return mandates.filter((m) => m.status === 'REVOKED');
  }
}

function scopeBadgeVariant(scope: Mandate['scope']): 'default' | 'outline' {
  return scope === 'PURCHASE' ? 'default' : 'outline';
}

function formatCurrency(amount: number): string {
  return `$${amount.toFixed(2)}`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString();
}

/**
 * Collapsible form for creating a new mandate.
 */
function CreateMandateForm({ onClose }: Readonly<{ onClose: () => void }>) {
  const [agentId, setAgentId] = useState('');
  const [scope, setScope] = useState<'BROWSE' | 'PURCHASE'>('BROWSE');
  const [maxSpendPerTransaction, setMaxSpendPerTransaction] = useState('');
  const [maxSpendTotal, setMaxSpendTotal] = useState('');
  const [allowedCategories, setAllowedCategories] = useState('');
  const [allowedEvents, setAllowedEvents] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const createMandate = useCreateMandate();

  const handleSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();

      if (!agentId.trim()) {
        toast.error('Agent ID is required.');
        return;
      }

      const request: CreateMandateRequest = {
        agentId: agentId.trim(),
        scope,
      };

      if (maxSpendPerTransaction) {
        const value = Number.parseFloat(maxSpendPerTransaction);
        if (!Number.isNaN(value) && value > 0) {
          request.maxSpendPerTransaction = value;
        }
      }
      if (maxSpendTotal) {
        const value = Number.parseFloat(maxSpendTotal);
        if (!Number.isNaN(value) && value > 0) {
          request.maxSpendTotal = value;
        }
      }
      if (allowedCategories.trim()) {
        request.allowedCategories = allowedCategories.trim();
      }
      if (allowedEvents.trim()) {
        request.allowedEvents = allowedEvents.trim();
      }
      if (expiresAt) {
        request.expiresAt = new Date(expiresAt).toISOString();
      }

      createMandate.mutate(request, {
        onSuccess: () => {
          toast.success('Mandate created.');
          onClose();
        },
        onError: () => {
          toast.error('Failed to create mandate.');
        },
      });
    },
    [
      agentId,
      scope,
      maxSpendPerTransaction,
      maxSpendTotal,
      allowedCategories,
      allowedEvents,
      expiresAt,
      createMandate,
      onClose,
    ],
  );

  return (
    <Card className="mb-6">
      <CardHeader>
        <CardTitle>New Mandate</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <label htmlFor="agentId" className="text-sm font-medium">
                Agent ID <span className="text-destructive">*</span>
              </label>
              <Input
                id="agentId"
                value={agentId}
                onChange={(e) => setAgentId(e.target.value)}
                placeholder="e.g., claude-desktop"
                required
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="scope" className="text-sm font-medium">
                Scope
              </label>
              <Select value={scope} onValueChange={(v) => setScope(v as 'BROWSE' | 'PURCHASE')}>
                <SelectTrigger id="scope">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="BROWSE">Browse</SelectItem>
                  <SelectItem value="PURCHASE">Purchase</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <label htmlFor="maxSpendPerTransaction" className="text-sm font-medium">
                Per-Transaction Limit
              </label>
              <Input
                id="maxSpendPerTransaction"
                type="number"
                min="0.01"
                step="0.01"
                value={maxSpendPerTransaction}
                onChange={(e) => setMaxSpendPerTransaction(e.target.value)}
                placeholder="$200.00"
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="maxSpendTotal" className="text-sm font-medium">
                Total Budget
              </label>
              <Input
                id="maxSpendTotal"
                type="number"
                min="0.01"
                step="0.01"
                value={maxSpendTotal}
                onChange={(e) => setMaxSpendTotal(e.target.value)}
                placeholder="$1,000.00"
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <label htmlFor="allowedCategories" className="text-sm font-medium">
                Allowed Categories
              </label>
              <Input
                id="allowedCategories"
                value={allowedCategories}
                onChange={(e) => setAllowedCategories(e.target.value)}
                placeholder="e.g., jazz, classical"
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="allowedEvents" className="text-sm font-medium">
                Allowed Events
              </label>
              <Input
                id="allowedEvents"
                value={allowedEvents}
                onChange={(e) => setAllowedEvents(e.target.value)}
                placeholder="e.g., yo-yo-ma-bach-suites"
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <label htmlFor="expiresAt" className="text-sm font-medium">
                Expires
              </label>
              <Input
                id="expiresAt"
                type="datetime-local"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
              />
            </div>
          </div>

          <div className="flex items-center gap-2 pt-2">
            <Button type="submit" disabled={createMandate.isPending}>
              {createMandate.isPending ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              ) : (
                <Shield className="mr-2 h-4 w-4" />
              )}
              Create Mandate
            </Button>
            <Button type="button" variant="ghost" onClick={onClose}>
              Cancel
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

/**
 * Spending summary line for a mandate.
 */
function SpendingSummary({ mandate }: Readonly<{ mandate: Mandate }>) {
  return (
    <div className="text-sm text-muted-foreground">
      {mandate.maxSpendPerTransaction !== null && (
        <span>{formatCurrency(mandate.maxSpendPerTransaction)} per transaction</span>
      )}
      {mandate.maxSpendPerTransaction !== null && mandate.maxSpendTotal !== null && (
        <span> &middot; </span>
      )}
      {mandate.maxSpendTotal !== null && (
        <span>
          {formatCurrency(mandate.totalSpent)} spent of {formatCurrency(mandate.maxSpendTotal)}{' '}
          total
          {mandate.remainingBudget !== null && (
            <span className="ml-1">({formatCurrency(mandate.remainingBudget)} remaining)</span>
          )}
        </span>
      )}
      {mandate.maxSpendPerTransaction === null && mandate.maxSpendTotal === null && (
        <span>No spending limits</span>
      )}
    </div>
  );
}

/**
 * Restrictions line for a mandate.
 */
function Restrictions({ mandate }: Readonly<{ mandate: Mandate }>) {
  if (!mandate.allowedCategories && !mandate.allowedEvents) {
    return null;
  }
  return (
    <div className="text-sm text-muted-foreground">
      {mandate.allowedCategories && <span>Categories: {mandate.allowedCategories}</span>}
      {mandate.allowedCategories && mandate.allowedEvents && <span> &middot; </span>}
      {mandate.allowedEvents && <span>Events: {mandate.allowedEvents}</span>}
    </div>
  );
}

/**
 * Card view for a single mandate, used on mobile screens.
 */
function MandateCard({
  mandate,
  onRevoke,
  isRevoking,
}: Readonly<{
  mandate: Mandate;
  onRevoke: (mandateId: string) => void;
  isRevoking: boolean;
}>) {
  const isActive =
    mandate.status === 'ACTIVE' &&
    (mandate.expiresAt === null || mandate.expiresAt > new Date().toISOString());

  return (
    <div className="rounded-lg border p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <p className="font-medium">{mandate.agentId}</p>
          <div className="mt-1 flex flex-wrap items-center gap-2">
            <Badge variant={scopeBadgeVariant(mandate.scope)}>{mandate.scope}</Badge>
            {mandate.status === 'REVOKED' && (
              <Badge variant="destructive">
                <Ban className="mr-1 h-3 w-3" />
                Revoked
              </Badge>
            )}
            {!isActive && mandate.status === 'ACTIVE' && (
              <Badge variant="secondary">
                <Clock className="mr-1 h-3 w-3" />
                Expired
              </Badge>
            )}
          </div>
        </div>
        {isActive && (
          <Button
            variant="destructive"
            size="sm"
            onClick={() => onRevoke(mandate.mandateId)}
            disabled={isRevoking}
          >
            {isRevoking ? <Loader2 className="h-3 w-3 animate-spin" /> : 'Revoke'}
          </Button>
        )}
      </div>

      <div className="mt-3 space-y-1">
        <SpendingSummary mandate={mandate} />
        <Restrictions mandate={mandate} />
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
        <span className="flex items-center gap-1">
          <Calendar className="h-3 w-3" />
          Created {formatDate(mandate.createdAt)}
        </span>
        {mandate.expiresAt && (
          <span className="flex items-center gap-1">
            <Clock className="h-3 w-3" />
            Expires {formatDate(mandate.expiresAt)}
          </span>
        )}
      </div>
    </div>
  );
}

/**
 * Mandates management page with tab filtering, create form, and responsive layout.
 * Uses a table on desktop and card layout on mobile.
 */
export function MandatesPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ACTIVE');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const revokeMandate = useRevokeMandate();

  const { data: mandates, isLoading } = useMyMandates();

  const handleRevoke = useCallback(
    (mandateId: string) => {
      revokeMandate.mutate(mandateId, {
        onSuccess: () => {
          toast.success('Mandate revoked.');
        },
        onError: () => {
          toast.error('Failed to revoke mandate.');
        },
      });
    },
    [revokeMandate],
  );

  if (isLoading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-6 sm:px-6 lg:px-8">
        <Skeleton className="mb-6 h-8 w-48" />
        <Skeleton className="mb-4 h-10 w-80" />
        <div className="space-y-3">
          {Array.from({ length: 5 }, (_, i) => i).map((n) => (
            <Skeleton key={`skeleton-${n}`} className="h-20 w-full" />
          ))}
        </div>
      </div>
    );
  }

  const allMandates = mandates ?? [];
  const filtered = filterMandates(allMandates, statusFilter);

  return (
    <div className="mx-auto max-w-5xl px-4 py-6 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Agent Mandates</h1>
        {!showCreateForm && (
          <Button onClick={() => setShowCreateForm(true)}>
            <Plus className="mr-2 h-4 w-4" />
            New Mandate
          </Button>
        )}
      </div>

      {showCreateForm && <CreateMandateForm onClose={() => setShowCreateForm(false)} />}

      <Tabs value={statusFilter} onValueChange={(value) => setStatusFilter(value as StatusFilter)}>
        <TabsList>
          <TabsTrigger value="ACTIVE">Active</TabsTrigger>
          <TabsTrigger value="EXPIRED">Expired</TabsTrigger>
          <TabsTrigger value="REVOKED">Revoked</TabsTrigger>
        </TabsList>

        <TabsContent value={statusFilter} className="mt-4">
          {filtered.length === 0 ? (
            <EmptyState
              icon={Shield}
              title="No mandates"
              description={
                statusFilter === 'ACTIVE'
                  ? 'Create a mandate to authorize an AI agent to act on your behalf.'
                  : `You have no ${statusFilter.toLowerCase()} mandates.`
              }
            />
          ) : (
            <>
              {/* Mobile: Card layout */}
              <div className="space-y-3 md:hidden">
                {filtered.map((mandate) => (
                  <MandateCard
                    key={mandate.id}
                    mandate={mandate}
                    onRevoke={handleRevoke}
                    isRevoking={revokeMandate.isPending}
                  />
                ))}
              </div>

              {/* Desktop: Table layout */}
              <div className="hidden md:block">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Agent</TableHead>
                      <TableHead>Scope</TableHead>
                      <TableHead>Spending</TableHead>
                      <TableHead>Restrictions</TableHead>
                      <TableHead>Expires</TableHead>
                      <TableHead>Created</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filtered.map((mandate) => {
                      const isActive =
                        mandate.status === 'ACTIVE' &&
                        (mandate.expiresAt === null ||
                          mandate.expiresAt > new Date().toISOString());
                      return (
                        <TableRow key={mandate.id}>
                          <TableCell className="font-medium">{mandate.agentId}</TableCell>
                          <TableCell>
                            <Badge variant={scopeBadgeVariant(mandate.scope)}>
                              {mandate.scope}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <SpendingSummary mandate={mandate} />
                          </TableCell>
                          <TableCell>
                            {mandate.allowedCategories || mandate.allowedEvents ? (
                              <div className="text-sm text-muted-foreground">
                                {mandate.allowedCategories && (
                                  <div>{mandate.allowedCategories}</div>
                                )}
                                {mandate.allowedEvents && <div>{mandate.allowedEvents}</div>}
                              </div>
                            ) : (
                              <span className="text-sm text-muted-foreground">None</span>
                            )}
                          </TableCell>
                          <TableCell>
                            {mandate.expiresAt ? (
                              formatDate(mandate.expiresAt)
                            ) : (
                              <span className="text-muted-foreground">Never</span>
                            )}
                          </TableCell>
                          <TableCell>{formatDate(mandate.createdAt)}</TableCell>
                          <TableCell className="text-right">
                            {isActive && (
                              <Button
                                variant="destructive"
                                size="sm"
                                onClick={() => handleRevoke(mandate.mandateId)}
                                disabled={revokeMandate.isPending}
                              >
                                {revokeMandate.isPending ? (
                                  <Loader2 className="h-3 w-3 animate-spin" />
                                ) : (
                                  'Revoke'
                                )}
                              </Button>
                            )}
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </div>
            </>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
