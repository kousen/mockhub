/**
 * Playwright script to capture seller flow screenshots for README.
 *
 * Run: npx playwright test e2e/capture-seller-screenshots.ts --project=chromium
 *
 * Saves screenshots to docs/screenshots/
 */
import { test, type Page } from '@playwright/test';
import { fileURLToPath } from 'url';
import path from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const SCREENSHOT_DIR = path.resolve(__dirname, '../../docs/screenshots');

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
      name: 'Taylor Swift | The Eras Tour',
      slug: 'taylor-swift-eras-tour',
      venueName: 'MetLife Stadium',
      city: 'East Rutherford',
      state: 'NJ',
      eventDate: '2026-07-18T19:00:00Z',
      category: 'CONCERT',
      status: 'ON_SALE',
      minPrice: 185.0,
      maxPrice: 1250.0,
      availableTickets: 1200,
      primaryImageUrl: null,
    },
    {
      id: 2,
      name: 'Kendrick Lamar | Grand National Tour',
      slug: 'kendrick-lamar-grand-national',
      venueName: 'Madison Square Garden',
      city: 'New York',
      state: 'NY',
      eventDate: '2026-08-05T20:00:00Z',
      category: 'CONCERT',
      status: 'ON_SALE',
      minPrice: 125.0,
      maxPrice: 450.0,
      availableTickets: 800,
      primaryImageUrl: null,
    },
    {
      id: 3,
      name: 'Hamilton',
      slug: 'hamilton-broadway',
      venueName: 'Richard Rodgers Theatre',
      city: 'New York',
      state: 'NY',
      eventDate: '2026-06-22T14:00:00Z',
      category: 'THEATER',
      status: 'ON_SALE',
      minPrice: 95.0,
      maxPrice: 350.0,
      availableTickets: 200,
      primaryImageUrl: null,
    },
  ],
  page: 0,
  size: 10,
  totalElements: 3,
  totalPages: 1,
};

const MOCK_MY_LISTINGS = [
  {
    id: 1,
    ticketId: 42,
    eventSlug: 'taylor-swift-eras-tour',
    eventName: 'Taylor Swift | The Eras Tour',
    eventDate: '2026-07-18T19:00:00Z',
    venueName: 'MetLife Stadium',
    sectionName: 'Floor A',
    rowLabel: '12',
    seatNumber: '8',
    ticketType: 'RESERVED',
    listedPrice: 425.0,
    computedPrice: 425.0,
    faceValue: 275.0,
    status: 'ACTIVE',
    listedAt: '2026-03-15T10:30:00Z',
    createdAt: '2026-03-15T10:30:00Z',
  },
  {
    id: 2,
    ticketId: 43,
    eventSlug: 'kendrick-lamar-grand-national',
    eventName: 'Kendrick Lamar | Grand National Tour',
    eventDate: '2026-08-05T20:00:00Z',
    venueName: 'Madison Square Garden',
    sectionName: 'Section 103',
    rowLabel: 'F',
    seatNumber: '15',
    ticketType: 'RESERVED',
    listedPrice: 195.0,
    computedPrice: 195.0,
    faceValue: 150.0,
    status: 'ACTIVE',
    listedAt: '2026-03-18T14:00:00Z',
    createdAt: '2026-03-18T14:00:00Z',
  },
  {
    id: 3,
    ticketId: 44,
    eventSlug: 'hamilton-broadway',
    eventName: 'Hamilton',
    eventDate: '2026-06-22T14:00:00Z',
    venueName: 'Richard Rodgers Theatre',
    sectionName: 'Orchestra',
    rowLabel: 'D',
    seatNumber: '101',
    ticketType: 'RESERVED',
    listedPrice: 310.0,
    computedPrice: 310.0,
    faceValue: 199.0,
    status: 'SOLD',
    listedAt: '2026-03-10T09:15:00Z',
    createdAt: '2026-03-10T09:15:00Z',
  },
  {
    id: 4,
    ticketId: 45,
    eventSlug: 'lakers-vs-celtics',
    eventName: 'Lakers vs Celtics',
    eventDate: '2026-04-12T19:30:00Z',
    venueName: 'Crypto.com Arena',
    sectionName: 'Section 112',
    rowLabel: 'J',
    seatNumber: '3',
    ticketType: 'RESERVED',
    listedPrice: 175.0,
    computedPrice: 175.0,
    faceValue: 120.0,
    status: 'CANCELLED',
    listedAt: '2026-03-05T16:45:00Z',
    createdAt: '2026-03-05T16:45:00Z',
  },
];

const MOCK_EARNINGS = {
  totalEarnings: 310.0,
  totalListings: 4,
  activeListings: 2,
  soldListings: 1,
  recentSales: [
    {
      orderNumber: 'MH-20260318-0047',
      eventName: 'Hamilton',
      sectionName: 'Orchestra',
      seatInfo: 'Orchestra, Row D, Seat 101',
      pricePaid: 310.0,
      soldAt: '2026-03-18T11:23:00Z',
    },
  ],
};

async function authenticateUser(page: Page) {
  await page.addInitScript((authState) => {
    sessionStorage.setItem('mockhub-auth', JSON.stringify(authState));
  }, MOCK_AUTH_STATE);
}

async function mockSellerEndpoints(page: Page) {
  await page.route('**/api/v1/events?*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_EVENTS),
    });
  });

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

  // Mock notifications to avoid 401 errors in the header
  await page.route(/\/api\/v1\/notifications/, async (route) => {
    if (route.request().url().includes('unread-count')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ count: 2 }),
      });
    } else {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [], totalElements: 0 }),
      });
    }
  });

  // Mock cart to avoid 401 errors
  await page.route(/\/api\/v1\/cart$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ id: 1, items: [], totalPrice: 0 }),
    });
  });

  // Mock favorites
  await page.route(/\/api\/v1\/favorites/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });
}

test.describe('Seller Screenshots', () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test('capture sell page - event search', async ({ page }) => {
    await authenticateUser(page);
    await mockSellerEndpoints(page);
    await page.goto('/sell');

    // Type a search to show results
    await page.getByPlaceholder('Search events...').fill('Taylor');
    await page.waitForTimeout(500);

    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, 'sell-page.png'),
      fullPage: false,
    });
  });

  test('capture my listings page', async ({ page }) => {
    await authenticateUser(page);
    await mockSellerEndpoints(page);
    await page.goto('/my/listings');

    // Wait for table data to load (desktop view)
    await page.locator('table').getByText('Taylor Swift').first().waitFor({ state: 'visible', timeout: 5000 });
    await page.waitForTimeout(300);

    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, 'my-listings.png'),
      fullPage: false,
    });
  });

  test('capture earnings page', async ({ page }) => {
    await authenticateUser(page);
    await mockSellerEndpoints(page);
    await page.goto('/my/earnings');

    // Wait for data to load
    await page.getByText('Total Earnings').waitFor({ state: 'visible', timeout: 5000 });
    await page.waitForTimeout(300);

    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, 'seller-earnings.png'),
      fullPage: false,
    });
  });
});
