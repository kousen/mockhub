import { cn } from '@/lib/utils';
import { formatCurrency } from '@/lib/formatters';

interface PriceTagProps {
  computedPrice: number;
  listedPrice: number;
  className?: string;
}

/**
 * Displays a price with visual formatting.
 * Shows computed price prominently. If different from listed price,
 * the listed price is shown struck through. Color indicates
 * whether the price is below (green) or above (red) the listed/face value.
 */
export function PriceTag({ computedPrice, listedPrice, className }: PriceTagProps) {
  const isDifferent = Math.abs(computedPrice - listedPrice) > 0.01;
  const isBelow = computedPrice < listedPrice;
  const isAbove = computedPrice > listedPrice;

  return (
    <div className={cn('flex items-center gap-2', className)}>
      <span
        className={cn(
          'font-semibold',
          isBelow && 'text-emerald-600 dark:text-emerald-400',
          isAbove && 'text-red-600 dark:text-red-400',
        )}
      >
        {formatCurrency(computedPrice)}
      </span>
      {isDifferent && (
        <span className="text-xs text-muted-foreground line-through">
          {formatCurrency(listedPrice)}
        </span>
      )}
    </div>
  );
}
