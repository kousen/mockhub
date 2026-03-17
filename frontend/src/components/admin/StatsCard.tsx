import { Card, CardContent } from '@/components/ui/card';
import { cn } from '@/lib/utils';

interface StatsCardProps {
  label: string;
  value: string | number;
  icon?: React.ElementType;
  trend?: number | null;
}

/**
 * Dashboard statistics card with label, value, optional icon, and trend indicator.
 */
export function StatsCard({ label, value, icon: Icon, trend }: StatsCardProps) {
  return (
    <Card>
      <CardContent className="flex items-center gap-4 p-6">
        {Icon && (
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-primary/10">
            <Icon className="h-6 w-6 text-primary" />
          </div>
        )}
        <div className="min-w-0 flex-1">
          <p className="text-sm text-muted-foreground">{label}</p>
          <p className="text-2xl font-bold">{value}</p>
          {trend !== undefined && trend !== null && (
            <p className={cn('mt-1 text-xs', trend >= 0 ? 'text-green-600' : 'text-red-600')}>
              {trend >= 0 ? '+' : ''}
              {trend.toFixed(1)}% from last period
            </p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
