import type { Cart } from '@/types/cart';

const STORAGE_KEY = 'mockhub-checkout-review-passed';

/**
 * Fingerprints the cart so the review gate auto-invalidates when items
 * are added/removed or prices change. Includes subtotal so a price drift
 * between review and payment forces another review.
 */
export function cartReviewFingerprint(cart: Cart): string {
  const itemKey = cart.items
    .map((item) => `${item.id}:${item.priceAtAdd}:${item.currentPrice}`)
    .join(',');
  return `${cart.id}|${cart.subtotal}|${itemKey}`;
}

export function markReviewPassed(cart: Cart): void {
  try {
    sessionStorage.setItem(STORAGE_KEY, cartReviewFingerprint(cart));
  } catch {
    // sessionStorage unavailable (private mode, etc.) makes the gate always block.
  }
}

export function hasReviewPassed(cart: Cart): boolean {
  try {
    return sessionStorage.getItem(STORAGE_KEY) === cartReviewFingerprint(cart);
  } catch {
    return false;
  }
}

export function clearReviewPassed(): void {
  try {
    sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    // no-op
  }
}
