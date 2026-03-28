import { test, expect } from '@playwright/test';

test.describe('Event browsing', () => {
  test('home page loads', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('banner').getByRole('link', { name: 'MockHub' })).toBeVisible();
  });

  test('events page renders browse heading', async ({ page }) => {
    await page.goto('/events');

    await expect(page.getByRole('heading', { name: /browse events/i })).toBeVisible();
  });

  test('events page has search input', async ({ page }) => {
    await page.goto('/events');

    const searchInput = page.getByPlaceholder(/search/i);
    await expect(searchInput).toBeVisible();
  });

  test('header navigation contains Events link', async ({ page, isMobile }) => {
    test.skip(!!isMobile, 'Desktop nav is hidden on mobile — tested separately');
    await page.goto('/');

    const eventsLink = page.getByRole('banner').getByRole('link', { name: 'Events' });
    await expect(eventsLink).toBeVisible();
  });

  test('clicking Events link navigates to events page', async ({ page, isMobile }) => {
    test.skip(!!isMobile, 'Desktop nav is hidden on mobile — tested separately');
    await page.goto('/');

    await page.getByRole('banner').getByRole('link', { name: 'Events' }).click();

    await expect(page).toHaveURL(/\/events/);
  });

  test('events page has sort options', async ({ page }) => {
    await page.goto('/events');

    await expect(page.getByRole('combobox', { name: 'Sort by' })).toBeVisible();
  });

  test('event cards are clickable and navigate to detail page', async ({ page }) => {
    const mockEventsList = {
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

    // Mock categories
    await page.route('**/api/v1/categories', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    // Mock tags
    await page.route('**/api/v1/tags', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
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

    // Mock events list — regex matches /api/v1/events with optional query params only
    await page.route(/\/api\/v1\/events(\?.*)?$/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockEventsList),
      });
    });

    await page.goto('/events');

    // Click the "View Tickets" link on the event card
    const viewTicketsLink = page.getByRole('link', { name: 'View Tickets' }).first();
    await expect(viewTicketsLink).toBeVisible({ timeout: 5000 });
    await viewTicketsLink.click();

    await expect(page).toHaveURL(/\/events\/rock-festival-2026/);
  });

  test('category filter buttons exist on events page', async ({ page }) => {
    // Mock categories
    await page.route('**/api/v1/categories', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { id: 1, name: 'Concerts', slug: 'concerts', icon: null, sortOrder: 1 },
          { id: 2, name: 'Sports', slug: 'sports', icon: null, sortOrder: 2 },
          { id: 3, name: 'Theater', slug: 'theater', icon: null, sortOrder: 3 },
        ]),
      });
    });

    await page.goto('/events');

    // CategoryNav renders a toolbar with category badges
    const toolbar = page.getByRole('toolbar', { name: 'Event categories' });
    await expect(toolbar).toBeVisible({ timeout: 5000 });

    // "All" is always present, plus the mocked categories
    await expect(toolbar.getByText('All')).toBeVisible();
    await expect(toolbar.getByText('Concerts')).toBeVisible();
    await expect(toolbar.getByText('Sports')).toBeVisible();
    await expect(toolbar.getByText('Theater')).toBeVisible();
  });
});
