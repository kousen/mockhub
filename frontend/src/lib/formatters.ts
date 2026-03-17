/**
 * Format a number as USD currency.
 */
export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
}

/**
 * Format a date string or Date object for display.
 * Example: "March 15, 2026 at 8:00 PM"
 */
export function formatDate(date: string | Date): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(d);
}

/**
 * Format a date string or Date object as a short date.
 * Example: "Mar 15, 2026"
 */
export function formatShortDate(date: string | Date): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(d);
}

/**
 * Format a date string or Date object as a relative time.
 * Example: "2 hours ago", "in 3 days"
 */
export function formatRelativeTime(date: string | Date): string {
  const d = typeof date === 'string' ? new Date(date) : date;
  const now = new Date();
  const diffMs = d.getTime() - now.getTime();
  const diffSec = Math.round(diffMs / 1000);
  const diffMin = Math.round(diffSec / 60);
  const diffHour = Math.round(diffMin / 60);
  const diffDay = Math.round(diffHour / 24);

  const rtf = new Intl.RelativeTimeFormat('en-US', { numeric: 'auto' });

  if (Math.abs(diffDay) >= 1) return rtf.format(diffDay, 'day');
  if (Math.abs(diffHour) >= 1) return rtf.format(diffHour, 'hour');
  if (Math.abs(diffMin) >= 1) return rtf.format(diffMin, 'minute');
  return rtf.format(diffSec, 'second');
}
