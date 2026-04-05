import { cn } from '@/lib/utils';

interface LoadingSpinnerProps {
  className?: string;
  size?: 'sm' | 'md' | 'lg';
}

const sizeClasses = {
  sm: 'h-4 w-4 border-2',
  md: 'h-8 w-8 border-2',
  lg: 'h-12 w-12 border-3',
} as const;

export function LoadingSpinner({ className, size = 'md' }: Readonly<LoadingSpinnerProps>) {
  return (
    <div className={cn('flex items-center justify-center', className)}>
      <output
        className={cn(
          'animate-spin rounded-full border-primary border-t-transparent',
          sizeClasses[size],
        )}
        aria-label="Loading"
      >
        <span className="sr-only">Loading...</span>
      </output>
    </div>
  );
}
