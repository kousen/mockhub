import { describe, it, expect, beforeEach } from 'vitest';
import type { Cart } from '@/types/cart';
import {
  cartReviewFingerprint,
  clearReviewPassed,
  hasReviewPassed,
  markReviewPassed,
} from './checkout-review-gate';

function makeCart(overrides: Partial<Cart> = {}): Cart {
  return {
    id: 1,
    userId: 1,
    items: [
      {
        id: 100,
        listingId: 10,
        eventName: 'Event A',
        eventSlug: 'event-a',
        sectionName: 'Floor',
        rowLabel: 'A',
        seatNumber: '1',
        ticketType: 'STANDARD',
        priceAtAdd: 100,
        currentPrice: 100,
        addedAt: '2026-05-01T10:00:00',
      },
    ],
    subtotal: 100,
    itemCount: 1,
    expiresAt: null,
    ...overrides,
  };
}

describe('checkout review gate', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('hasReviewPassed is false before marking', () => {
    expect(hasReviewPassed(makeCart())).toBe(false);
  });

  it('marks and detects review passed for the same cart fingerprint', () => {
    const cart = makeCart();
    markReviewPassed(cart);
    expect(hasReviewPassed(cart)).toBe(true);
  });

  it('invalidates the gate when cart subtotal changes', () => {
    const cart = makeCart();
    markReviewPassed(cart);
    const repriced = makeCart({ subtotal: 150 });
    expect(hasReviewPassed(repriced)).toBe(false);
  });

  it('invalidates the gate when an item changes price', () => {
    const cart = makeCart();
    markReviewPassed(cart);
    const repriced = makeCart({
      items: [{ ...cart.items[0], currentPrice: 120 }],
    });
    expect(hasReviewPassed(repriced)).toBe(false);
  });

  it('invalidates the gate when the since-added price signal changes', () => {
    const cart = makeCart();
    markReviewPassed(cart);
    const changedSignal = makeCart({
      items: [{ ...cart.items[0], priceAtAdd: 90 }],
    });
    expect(hasReviewPassed(changedSignal)).toBe(false);
  });

  it('invalidates the gate when items are added', () => {
    const cart = makeCart();
    markReviewPassed(cart);
    const withMore = makeCart({
      items: [cart.items[0], { ...cart.items[0], id: 101, listingId: 11 }],
      itemCount: 2,
      subtotal: 200,
    });
    expect(hasReviewPassed(withMore)).toBe(false);
  });

  it('clearReviewPassed removes the marker', () => {
    const cart = makeCart();
    markReviewPassed(cart);
    clearReviewPassed();
    expect(hasReviewPassed(cart)).toBe(false);
  });

  it('cartReviewFingerprint is stable for equivalent carts', () => {
    expect(cartReviewFingerprint(makeCart())).toBe(cartReviewFingerprint(makeCart()));
  });
});
