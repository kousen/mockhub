import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

// -- Mock data --

const MOCK_USER = {
  id: 2,
  email: 'seller@mockhub.com',
  firstName: 'John',
  lastName: 'Seller',
  phone: '555-0102',
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

const MOCK_EVENTS = {
  content: [
    {
      id: 1,
      name: 'Rock Festival 2026',
      slug: 'rock-festival-2026',
      venueName: 'Madison Square Garden',
      city: 'New York',
      state: 'NY',
      eventDate: '2026-06-15T19:00:00Z',
      category: 'CONCERT',
      status: 'ON_SALE',
      minPrice: 75.0,
      maxPrice: 250.0,
      availableTickets: 500,
      primaryImageUrl: null,
    },
  ],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
};

const MOCK_SELLER_LISTING = {
  id: 1,
  ticketId: 42,
  eventSlug: 'rock-festival-2026',
  eventName: 'Rock Festival 2026',
  eventDate: '2026-06-15T19:00:00Z',
  venueName: 'Madison Square Garden',
  sectionName: 'Floor',
  rowLabel: 'A',
  seatNumber: '5',
  ticketType: 'RESERVED',
  listedPrice: 120.0,
  computedPrice: 120.0,
  faceValue: 75.0,
  status: 'ACTIVE',
  listedAt: '2026-03-20T12:00:00Z',
  createdAt: '2026-03-20T12:00:00Z',
};

const MOCK_MY_LISTINGS = [
  MOCK_SELLER_LISTING,
  {
    ...MOCK_SELLER_LISTING,
    id: 2,
    ticketId: 43,
    seatNumber: '6',
    status: 'SOLD',
    listedPrice: 100.0,
    computedPrice: 100.0,
  },
];

const MOCK_EARNINGS = {
  totalEarnings: 100.0,
  totalListings: 2,
  activeListings: 1,
  soldListings: 1,
  recentSales: [
    {
      orderNumber: 'MH-20260320-0001',
      eventName: 'Rock Festival 2026',
      sectionName: 'Floor',
      seatInfo: 'Floor, Row A, Seat 6',
      pricePaid: 100.0,
      soldAt: '2026-03-20T14:00:00Z',
    },
  ],
};

// -- Helpers --

async function authenticateUser(page: Page) {
  await page.addInitScript((authState) => {
    sessionStorage.setItem('mockhub-auth', JSON.stringify(authState));
  }, MOCK_AUTH_STATE);

  // Mock background API calls that fire on any authenticated page.
  // Without these, the mock JWT hits the real backend, gets 401,
  // triggers a token refresh cascade, and redirects to /login.
  await page.route(/\/api\/v1\/notifications/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }),
    });
  });

  await page.route(/\/api\/v1\/cart$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items: [], totalPrice: 0 }),
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

async function mockSellerEndpoints(page: Page) {
  // Events search (for sell page event picker)
  await page.route('**/api/v1/events?*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_EVENTS),
    });
  });

  // Owned tickets for sell page (empty by default so tests exercise the search flow)
  await page.route(/\/api\/v1\/my\/owned-tickets/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  // Use regex for precise matching — /my/ endpoints first to avoid greedy glob conflicts
  await page.route(/\/api\/v1\/my\/listings/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_MY_LISTINGS),
    });
  });

  await page.route(/\/api\/v1\/my\/earnings$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_EARNINGS),
    });
  });

  // PUT /listings/{id}/price
  await page.route(/\/api\/v1\/listings\/\d+\/price$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ ...MOCK_SELLER_LISTING, listedPrice: 150.0, computedPrice: 150.0 }),
    });
  });

  // DELETE /listings/{id}
  await page.route(/\/api\/v1\/listings\/\d+$/, async (route) => {
    if (route.request().method() === 'DELETE') {
      await route.fulfill({ status: 204 });
    } else {
      await route.continue();
    }
  });

  // POST /listings (create listing — exact path, no subpaths)
  await page.route(/\/api\/v1\/listings$/, async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_SELLER_LISTING),
      });
    } else {
      await route.continue();
    }
  });
}

// -- Tests --

test.describe('Seller Flow', () => {
  test.describe('Navigation', () => {
    test('sell link is visible when authenticated', async ({ page, isMobile }) => {
      test.skip(!!isMobile, 'Desktop nav tested — mobile nav tested separately');
      await authenticateUser(page);
      await page.goto('/');

      await expect(page.getByRole('banner').getByRole('link', { name: 'Sell' })).toBeVisible();
    });

    test('sell link is not visible when unauthenticated', async ({ page, isMobile }) => {
      test.skip(!!isMobile, 'Desktop nav tested — mobile nav tested separately');
      await page.goto('/');

      await expect(page.getByRole('banner').getByRole('link', { name: 'Sell' })).not.toBeVisible();
    });

    test('unauthenticated user is redirected from /sell', async ({ page }) => {
      await page.goto('/sell');

      await expect(page).not.toHaveURL('/sell');
    });

    test('unauthenticated user is redirected from /my/listings', async ({ page }) => {
      await page.goto('/my/listings');

      await expect(page).not.toHaveURL('/my/listings');
    });

    test('unauthenticated user is redirected from /my/earnings', async ({ page }) => {
      await page.goto('/my/earnings');

      await expect(page).not.toHaveURL('/my/earnings');
    });
  });

  test.describe('Sell Tickets Page', () => {
    test('sell page renders step 1 - event search', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/sell');

      await expect(page.getByRole('heading', { name: 'Sell Tickets' })).toBeVisible();
      await expect(page.getByPlaceholder('Search events...')).toBeVisible();
    });

    test('searching for events shows results', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/sell');

      await page.getByPlaceholder('Search events...').fill('Rock');

      await expect(page.getByText('Rock Festival 2026')).toBeVisible({ timeout: 5000 });
    });

    test('selecting event advances to step 2 - seat details', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/sell');

      await page.getByPlaceholder('Search events...').fill('Rock');
      await page.getByText('Rock Festival 2026').click();

      await expect(page.getByLabel('Section')).toBeVisible();
      await expect(page.getByLabel('Row')).toBeVisible();
      await expect(page.getByLabel('Seat Number')).toBeVisible();
    });

    test('filling seat details advances to step 3 - set price', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/sell');

      // Step 1: Select event
      await page.getByPlaceholder('Search events...').fill('Rock');
      await page.getByText('Rock Festival 2026').click();

      // Step 2: Enter seat details
      await page.getByLabel('Section').fill('Floor');
      await page.getByLabel('Row').fill('A');
      await page.getByLabel('Seat Number').fill('5');
      await page.getByRole('button', { name: 'Continue' }).click();

      // Step 3: Price form
      await expect(page.getByLabel('Listing Price ($)')).toBeVisible();
      await expect(page.getByRole('button', { name: 'Create Listing' })).toBeVisible();
    });

    test('full sell flow creates listing and redirects', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/sell');

      // Step 1: Select event
      await page.getByPlaceholder('Search events...').fill('Rock');
      await page.getByText('Rock Festival 2026').click();

      // Step 2: Seat details
      await page.getByLabel('Section').fill('Floor');
      await page.getByLabel('Row').fill('A');
      await page.getByLabel('Seat Number').fill('5');
      await page.getByRole('button', { name: 'Continue' }).click();

      // Step 3: Set price and submit
      await page.getByLabel('Listing Price ($)').fill('120');
      await page.getByRole('button', { name: 'Create Listing' }).click();

      // Should show success toast and redirect
      await expect(page.getByText('Listing created successfully!')).toBeVisible({
        timeout: 5000,
      });
      await expect(page).toHaveURL(/\/my\/listings/, { timeout: 5000 });
    });

    test('back button returns to previous step', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/sell');

      // Advance to step 2
      await page.getByPlaceholder('Search events...').fill('Rock');
      await page.getByText('Rock Festival 2026').click();
      await expect(page.getByLabel('Section')).toBeVisible();

      // Go back to step 1
      await page.getByRole('button', { name: 'Back' }).click();
      await expect(page.getByPlaceholder('Search events...')).toBeVisible();
    });
  });

  test.describe('My Listings Page', () => {
    test('my listings page renders with tabs', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/my/listings');

      await expect(page.getByRole('heading', { name: 'My Listings' })).toBeVisible();
      await expect(page.getByRole('tab', { name: 'All' })).toBeVisible();
      await expect(page.getByRole('tab', { name: 'Active' })).toBeVisible();
      await expect(page.getByRole('tab', { name: 'Sold' })).toBeVisible();
      await expect(page.getByRole('tab', { name: 'Cancelled' })).toBeVisible();
    });

    test('listings show event name and price', async ({ page, isMobile }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/my/listings');

      if (isMobile) {
        // Mobile: card layout
        await expect(page.getByText('Rock Festival 2026').first()).toBeVisible({
          timeout: 5000,
        });
        await expect(page.getByText('$120.00').first()).toBeVisible();
      } else {
        // Desktop: table layout
        const table = page.locator('table');
        await expect(table.getByText('Rock Festival 2026').first()).toBeVisible({
          timeout: 5000,
        });
        await expect(table.getByText('$120.00').first()).toBeVisible();
      }
    });

    test('sell tickets button links to sell page', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/my/listings');

      const sellButton = page.getByRole('link', { name: /sell tickets/i });
      await expect(sellButton).toBeVisible();
      await expect(sellButton).toHaveAttribute('href', '/sell');
    });
  });

  test.describe('Earnings Page', () => {
    test('earnings page renders summary cards', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/my/earnings');

      await expect(page.getByRole('heading', { name: 'Earnings' })).toBeVisible();
      await expect(page.getByText('Total Earnings')).toBeVisible();
      await expect(page.getByText('$100.00').first()).toBeVisible();
      await expect(page.getByText('Active Listings')).toBeVisible();
      await expect(page.getByText('Sold Listings')).toBeVisible();
    });

    test('earnings page shows recent sales', async ({ page, isMobile }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/my/earnings');

      await expect(page.getByText('Recent Sales')).toBeVisible();

      if (isMobile) {
        await expect(page.getByText('Rock Festival 2026').first()).toBeVisible({
          timeout: 5000,
        });
      } else {
        const table = page.locator('table');
        await expect(table.getByText('Rock Festival 2026').first()).toBeVisible({
          timeout: 5000,
        });
      }
    });
  });

  test.describe('Accessibility', () => {
    test('sell page has no critical accessibility violations', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/sell');
      await page.waitForLoadState('domcontentloaded');

      const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();

      const criticalViolations = results.violations.filter(
        (v) => v.impact === 'critical' || v.impact === 'serious',
      );

      expect(criticalViolations).toHaveLength(0);
    });

    test('my listings page has no critical accessibility violations', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/my/listings');
      await page.waitForLoadState('domcontentloaded');

      const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();

      const criticalViolations = results.violations.filter(
        (v) => v.impact === 'critical' || v.impact === 'serious',
      );

      expect(criticalViolations).toHaveLength(0);
    });

    test('earnings page has no critical accessibility violations', async ({ page }) => {
      await authenticateUser(page);
      await mockSellerEndpoints(page);
      await page.goto('/my/earnings');
      await page.waitForLoadState('domcontentloaded');

      const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();

      const criticalViolations = results.violations.filter(
        (v) => v.impact === 'critical' || v.impact === 'serious',
      );

      expect(criticalViolations).toHaveLength(0);
    });
  });
});
