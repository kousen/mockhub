import { cn } from '@/lib/utils';
import { formatCurrency } from '@/lib/formatters';

interface PriceDisplayProps {
  price: number;
  originalPrice?: number;
  prefix?: string;
  className?: string;
}

/**
 * Reusable price display with currency formatting.
 * Supports optional "From" prefix for price ranges and
 * strikethrough styling for original prices when showing a sale.
 */
export function PriceDisplay({ price, originalPrice, prefix, className }: PriceDisplayProps) {
  const hasDiscount = originalPrice !== undefined && Math.abs(originalPrice - price) > 0.01;
  const isBelow = hasDiscount && price < originalPrice;

  return (
    <div className={cn('flex items-center gap-1.5', className)}>
      {prefix && (
        <span className="text-xs text-muted-foreground">{prefix}</span>
      )}
      <span
        className={cn(
          'font-semibold',
          isBelow && 'text-emerald-600 dark:text-emerald-400',
          hasDiscount && !isBelow && 'text-red-600 dark:text-red-400',
        )}
      >
        {formatCurrency(price)}
      </span>
      {hasDiscount && (
        <span className="text-xs text-muted-foreground line-through">
          {formatCurrency(originalPrice)}
        </span>
      )}
    </div>
  );
}
