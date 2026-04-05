import { ArrowDown, ArrowRight, ArrowUp, TrendingUp } from 'lucide-react';
import { usePricePrediction } from '@/hooks/use-ai';
import { formatCurrency } from '@/lib/formatters';
import type { PriceTrend } from '@/types/ai';

interface PricePredictionBadgeProps {
  slug: string;
}

const trendConfig: Record<
  PriceTrend,
  { label: string; icon: typeof ArrowUp; colorClass: string; bgClass: string }
> = {
  RISING: {
    label: 'Rising',
    icon: ArrowUp,
    colorClass: 'text-red-600 dark:text-red-400',
    bgClass: 'bg-red-50 border-red-200 dark:bg-red-950/30 dark:border-red-800',
  },
  FALLING: {
    label: 'Falling',
    icon: ArrowDown,
    colorClass: 'text-emerald-600 dark:text-emerald-400',
    bgClass: 'bg-emerald-50 border-emerald-200 dark:bg-emerald-950/30 dark:border-emerald-800',
  },
  STABLE: {
    label: 'Stable',
    icon: ArrowRight,
    colorClass: 'text-muted-foreground',
    bgClass: 'bg-muted/50 border-border',
  },
};

/**
 * Displays AI-predicted price trend for an event.
 * Shows predicted price, trend direction, and confidence level.
 * Renders nothing if AI is unavailable or prediction data is missing.
 */
export function PricePredictionBadge({ slug }: Readonly<PricePredictionBadgeProps>) {
  const { data: prediction } = usePricePrediction(slug);

  if (!prediction) {
    return null;
  }

  const config = trendConfig[prediction.trend];
  const TrendIcon = config.icon;

  return (
    <div className={`rounded-lg border p-3 ${config.bgClass}`}>
      <div className="flex items-center gap-2">
        <TrendingUp className={`h-4 w-4 ${config.colorClass}`} />
        <span className="text-xs font-medium">AI Price Prediction</span>
      </div>
      <div className="mt-2 flex items-baseline gap-3">
        <span className={`text-lg font-semibold ${config.colorClass}`}>
          {formatCurrency(prediction.predictedPrice)}
        </span>
        <div className={`flex items-center gap-1 text-xs font-medium ${config.colorClass}`}>
          <TrendIcon className="h-3 w-3" />
          {config.label}
        </div>
      </div>
      <div className="mt-1 flex items-center justify-between text-xs text-muted-foreground">
        <span>Confidence: {Math.round(prediction.confidence * 100)}%</span>
        <span>Current: {formatCurrency(prediction.currentPrice)}</span>
      </div>
    </div>
  );
}
