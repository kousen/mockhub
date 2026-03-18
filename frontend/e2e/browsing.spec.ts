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
});
