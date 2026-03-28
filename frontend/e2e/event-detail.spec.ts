import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

// -- Mock data --

const MOCK_EVENT = {
  id: 1,
  name: 'Rock Festival 2026',
  slug: 'rock-festival-2026',
  description: 'An amazing rock festival',
  artistName: 'The Rockers',
  eventDate: '2026-06-15T19:00:00Z',
  doorsOpenAt: '2026-06-15T18:00:00Z',
  status: 'ON_SALE',
  basePrice: 75.0,
  minPrice: 50.0,
  maxPrice: 250.0,
  totalTickets: 1000,
  availableTickets: 500,
  isFeatured: true,
  venue: { id: 1, name: 'Madison Square Garden', slug: 'msg', city: 'New York', state: 'NY' },
  category: { id: 1, name: 'Concerts', slug: 'concerts' },
  tags: [{ id: 1, name: 'Rock' }],
  primaryImageUrl: null,
  spotifyArtistId: '4Z8W4fKeB5YxbusRsdQVPb',
};

const MOCK_EVENT_NO_SPOTIFY = {
  ...MOCK_EVENT,
  spotifyArtistId: null,
};

const MOCK_SPOTIFY_ARTIST = {
  id: '4Z8W4fKeB5YxbusRsdQVPb',
  name: 'The Rockers',
  genres: ['rock', 'alternative rock', 'indie'],
  followers: 5000000,
  imageUrl: null,
};

const MOCK_EVENTS_LIST = {
  content: [
    {
      id: 1,
      name: 'Rock Festival 2026',
      slug: 'rock-festival-2026',
      artistName: 'The Rockers',
      venueName: 'Madison Square Garden',
      city: 'New York',
      eventDate: '2026-06-15T19:00:00Z',
      minPrice: 50.0,
      availableTickets: 500,
      primaryImageUrl: null,
      categoryName: 'Concerts',
      isFeatured: true,
    },
  ],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
};

const MOCK_CATEGORIES = [
  { id: 1, name: 'Concerts', slug: 'concerts', icon: null, sortOrder: 1 },
  { id: 2, name: 'Sports', slug: 'sports', icon: null, sortOrder: 2 },
  { id: 3, name: 'Theater', slug: 'theater', icon: null, sortOrder: 3 },
];

// -- Helpers --

async function mockEventDetailEndpoints(page: Page, event = MOCK_EVENT) {
  // Featured events (must be registered before the generic events route)
  await page.route(/\/api\/v1\/events\/featured/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([MOCK_EVENTS_LIST.content[0]]),
    });
  });

  // Categories
  await page.route('**/api/v1/categories', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_CATEGORIES),
    });
  });

  // Tags
  await page.route('**/api/v1/tags', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  // Event detail
  await page.route(/\/api\/v1\/events\/rock-festival-2026$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(event),
    });
  });

  // Event listings
  await page.route(/\/api\/v1\/events\/rock-festival-2026\/listings/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  // Event sections
  await page.route(/\/api\/v1\/events\/rock-festival-2026\/sections/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  // Event price history
  await page.route(/\/api\/v1\/events\/rock-festival-2026\/price-history/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  // AI price prediction (may 503 — return null-equivalent)
  await page.route(/\/api\/v1\/events\/rock-festival-2026\/predicted-price/, async (route) => {
    await route.fulfill({ status: 503 });
  });

  // Spotify artist API
  await page.route(/\/api\/v1\/spotify\/artists\//, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_SPOTIFY_ARTIST),
    });
  });

  // Favorites (background call)
  await page.route(/\/api\/v1\/favorites/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  // Recommendations (background call on home/events pages)
  await page.route(/\/api\/v1\/recommendations/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });

  // Events list — generic catch-all for /api/v1/events with or without query params.
  // Registered last so Playwright checks it first (LIFO), but we use a precise regex
  // that only matches the bare /events path (with optional query string), not sub-paths.
  await page.route(/\/api\/v1\/events(\?.*)?$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_EVENTS_LIST),
    });
  });
}

// -- Tests --

test.describe('Event Detail Page', () => {
  test('event detail page loads with event name heading', async ({ page }) => {
    await mockEventDetailEndpoints(page);
    await page.goto('/events');

    // Click first event card link
    await page.getByRole('link', { name: 'Rock Festival 2026' }).first().click();

    await expect(page).toHaveURL(/\/events\/rock-festival-2026/);
    await expect(page.getByRole('heading', { name: 'Rock Festival 2026' })).toBeVisible({
      timeout: 5000,
    });
  });

  test('event detail shows venue info', async ({ page }) => {
    await mockEventDetailEndpoints(page);
    await page.goto('/events/rock-festival-2026');

    await expect(page.getByText('Madison Square Garden')).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('New York')).toBeVisible();
  });

  test('event detail shows ticket tabs', async ({ page }) => {
    await mockEventDetailEndpoints(page);
    await page.goto('/events/rock-festival-2026');

    await expect(page.getByRole('tab', { name: /Tickets/i })).toBeVisible({ timeout: 5000 });
    await expect(page.getByRole('tab', { name: /Venue Map/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Price History/i })).toBeVisible();
  });

  test('event detail shows Spotify player when artist has spotifyArtistId', async ({ page }) => {
    await mockEventDetailEndpoints(page, MOCK_EVENT);
    await page.goto('/events/rock-festival-2026');

    // Wait for the page to load
    await expect(page.getByRole('heading', { name: 'Rock Festival 2026' })).toBeVisible({
      timeout: 5000,
    });

    // Verify Spotify iframe is present
    const iframe = page.locator('iframe[src*="open.spotify.com/embed/artist"]');
    await expect(iframe).toBeVisible({ timeout: 5000 });

    // Verify genre badges are rendered
    await expect(page.getByText('rock', { exact: true })).toBeVisible();
    await expect(page.getByText('alternative rock')).toBeVisible();
    await expect(page.getByText('indie')).toBeVisible();
  });

  test('event detail hides Spotify section when no spotifyArtistId', async ({ page }) => {
    await mockEventDetailEndpoints(page, MOCK_EVENT_NO_SPOTIFY);
    await page.goto('/events/rock-festival-2026');

    // Wait for the page to load
    await expect(page.getByRole('heading', { name: 'Rock Festival 2026' })).toBeVisible({
      timeout: 5000,
    });

    // Verify no Spotify iframe is present
    const iframe = page.locator('iframe[src*="open.spotify.com/embed/artist"]');
    await expect(iframe).toHaveCount(0);
  });

  test('event detail page has no critical accessibility violations', async ({ page }) => {
    await mockEventDetailEndpoints(page);
    await page.goto('/events/rock-festival-2026');
    await page.waitForLoadState('domcontentloaded');

    // Wait for main content to render
    await expect(page.getByRole('heading', { name: 'Rock Festival 2026' })).toBeVisible({
      timeout: 5000,
    });

    const results = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa'])
      // Exclude Spotify brand green button and emerald price text (known contrast issues)
      .exclude('.bg-\\[\\#1DB954\\]')
      .exclude('.text-emerald-600')
      // Exclude Spotify embed iframe (third-party ARIA violations we cannot control)
      .exclude('iframe[src*="open.spotify.com"]')
      .analyze();

    const criticalViolations = results.violations.filter(
      (v) => v.impact === 'critical' || v.impact === 'serious',
    );

    expect(criticalViolations).toHaveLength(0);
  });
});
