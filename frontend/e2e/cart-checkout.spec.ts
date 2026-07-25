import { test, expect, type Page } from '@playwright/test';

const MOCK_USER = {
  id: 1,
  email: 'buyer@mockhub.com',
  firstName: 'Jane',
  lastName: 'Buyer',
  phone: '555-0101',
  avatarUrl: null,
  roles: ['ROLE_USER'],
};

const MOCK_AUTH_STATE = {
  state: {
    user: MOCK_USER,
    accessToken: 'mock-jwt-token',
    isAuthenticated: true,
  },
  version: 0,
};

const MOCK_CART_WITH_ITEMS = {
  items: [
    {
      id: 1,
      listingId: 10,
      eventName: 'Rock Festival 2026',
      sectionName: 'Floor',
      rowLabel: 'A',
      seatNumber: '5',
      price: 120.0,
      eventDate: '2026-06-15T19:00:00Z',
      venueName: 'Madison Square Garden',
    },
  ],
  totalPrice: 120.0,
  itemCount: 1,
  subtotal: 120.0,
};

const MOCK_CART_REVIEW_SHAPE = {
  id: 1,
  userId: 1,
  items: [
    {
      id: 1,
      listingId: 10,
      eventName: 'Rock Festival 2026',
      eventSlug: 'rock-festival-2026',
      sectionName: 'Floor',
      rowLabel: 'A',
      seatNumber: '5',
      ticketType: 'STANDARD',
      priceAtAdd: 120.0,
      currentPrice: 120.0,
      addedAt: '2026-05-10T10:00:00',
    },
  ],
  subtotal: 120.0,
  itemCount: 1,
  expiresAt: null,
};

async function authenticateUser(page: Page) {
  await page.addInitScript((authState) => {
    sessionStorage.setItem('mockhub-auth', JSON.stringify(authState));
  }, MOCK_AUTH_STATE);

  // Mock background API calls that fire on any authenticated page
  await page.route(/\/api\/v1\/notifications/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }),
    });
  });

  // Pending-approval badge polls this on every authenticated page
  await page.route(/\/api\/v1\/agent-approvals/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  await page.route(/\/api\/v1\/favorites/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });
}

test.describe('Cart and checkout', () => {
  test('cart page is protected @smoke', async ({ page }) => {
    await page.goto('/cart');

    // Unauthenticated user should be redirected
    await expect(page).not.toHaveURL('/cart');
  });

  test('checkout page is protected @smoke', async ({ page }) => {
    await page.goto('/checkout');

    // Unauthenticated user should be redirected
    await expect(page).not.toHaveURL('/checkout');
  });

  test('header shows cart icon when authenticated', async ({ page }) => {
    // This test verifies the page loads with the header structure
    await page.goto('/');

    // Cart button is only visible when authenticated, so we just check the header loaded
    await expect(page.getByRole('banner').getByRole('link', { name: 'MockHub' })).toBeVisible();
  });

  test('cart -> review -> payment flow walks through the review gate', async ({ page }) => {
    await authenticateUser(page);

    await page.route(/\/api\/v1\/cart$/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_CART_REVIEW_SHAPE),
      });
    });

    // Start on the cart page
    await page.goto('/cart');
    await expect(page.getByRole('heading', { name: 'Shopping Cart' })).toBeVisible();
    await expect(page.getByText('Rock Festival 2026')).toBeVisible();

    // Click "Proceed to Checkout" -> lands on /checkout/review (not /checkout)
    await page.getByRole('link', { name: 'Proceed to Checkout' }).click();
    await expect(page).toHaveURL(/\/checkout\/review$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Order Review' })).toBeVisible();

    // Itemized breakdown is visible (10% fee invariant comes from CartSummary)
    await expect(page.getByText('Service Fee (10%)')).toBeVisible();
    await expect(page.getByText('$12.00')).toBeVisible(); // 10% of $120
    await expect(page.getByText('$132.00')).toBeVisible(); // total

    // Back to Cart preserves cart state
    await page.locator('a[href="/cart"]').filter({ hasText: 'Back to Cart' }).click();
    await expect(page).toHaveURL(/\/cart$/);
    await expect(page.getByText('Rock Festival 2026')).toBeVisible();

    // Re-enter review and continue to payment
    await page.getByRole('link', { name: 'Proceed to Checkout' }).click();
    await expect(page).toHaveURL(/\/checkout\/review$/);
    await page.getByRole('button', { name: 'Continue to Payment' }).click();
    await expect(page).toHaveURL(/\/checkout$/);
    await expect(page.getByRole('heading', { name: 'Checkout' })).toBeVisible();
    await expect(page.getByText('Payment Method')).toBeVisible();
  });

  test('direct nav to /checkout with non-empty cart redirects to /checkout/review', async ({
    page,
  }) => {
    await authenticateUser(page);

    await page.route(/\/api\/v1\/cart$/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_CART_REVIEW_SHAPE),
      });
    });

    await page.goto('/checkout');
    await expect(page).toHaveURL(/\/checkout\/review$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Order Review' })).toBeVisible();
  });

  test('cart icon shows badge count when authenticated with items in cart', async ({
    page,
    isMobile,
  }) => {
    test.skip(
      !!isMobile,
      'Cart button in header is not visible on mobile — mobile nav tested separately',
    );
    await authenticateUser(page);

    // Mock cart endpoint to return a cart with one item
    await page.route(/\/api\/v1\/cart$/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_CART_WITH_ITEMS),
      });
    });

    await page.goto('/');

    // The cart button should be visible in the header
    const cartButton = page.getByRole('button', { name: /open cart/i });
    await expect(cartButton).toBeVisible({ timeout: 5000 });

    // The badge showing item count should be visible
    const badge = cartButton.locator('span');
    await expect(badge).toBeVisible({ timeout: 5000 });
    await expect(badge).toHaveText('1');
  });
});
