import { describe, it, expect, vi } from 'vitest';
import { getCart, addToCart, removeFromCart, clearCart } from './cart';

vi.mock('./client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: 'mock-cart' }),
    post: vi.fn().mockResolvedValue({ data: 'mock-cart' }),
    delete: vi.fn().mockResolvedValue({ data: 'mock-cart' }),
  },
}));

describe('cart API', () => {
  it('getCart calls /cart', async () => {
    const client = (await import('./client')).default;
    const result = await getCart();
    expect(client.get).toHaveBeenCalledWith('/cart');
    expect(result).toBe('mock-cart');
  });

  it('addToCart posts to /cart/items', async () => {
    const client = (await import('./client')).default;
    await addToCart({ listingId: 1 });
    expect(client.post).toHaveBeenCalledWith('/cart/items', { listingId: 1 });
  });

  it('removeFromCart deletes /cart/items/:id', async () => {
    const client = (await import('./client')).default;
    await removeFromCart(5);
    expect(client.delete).toHaveBeenCalledWith('/cart/items/5');
  });

  it('clearCart deletes /cart', async () => {
    const client = (await import('./client')).default;
    await clearCart();
    expect(client.delete).toHaveBeenCalledWith('/cart');
  });
});
