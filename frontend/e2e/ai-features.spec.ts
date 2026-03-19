import { test, expect } from '@playwright/test';

// AI tests need longer timeouts since they wait for real LLM responses.
// Run serially to avoid overwhelming the AI provider with concurrent requests.
test.describe('AI Features', () => {
  test.describe.configure({ mode: 'serial' });
  test.describe('Chat Widget', () => {
    test('chat button is visible on homepage', async ({ page }) => {
      await page.goto('/');

      const chatButton = page.getByRole('button', { name: /open ai chat assistant/i });
      await expect(chatButton).toBeVisible();
    });

    test('clicking chat button opens chat panel', async ({ page }) => {
      await page.goto('/');

      await page.getByRole('button', { name: /open ai chat assistant/i }).click();

      await expect(page.getByPlaceholder(/ask about events/i)).toBeVisible();
    });

    test('sending a message returns an AI response', async ({ page }) => {
      test.setTimeout(60000);
      await page.goto('/');

      await page.getByRole('button', { name: /open ai chat assistant/i }).click();

      const input = page.getByPlaceholder(/ask about events/i);
      await input.fill('What events are available?');
      await input.press('Enter');

      // Wait for AI response (may take a few seconds with Haiku)
      const aiMessage = page.locator('[data-testid="ai-message"]').first();
      await expect(aiMessage).toBeVisible({ timeout: 45000 });
      await expect(aiMessage).not.toBeEmpty();
    });

    test('chat panel can be closed', async ({ page }) => {
      await page.goto('/');

      await page.getByRole('button', { name: /open ai chat assistant/i }).click();
      await expect(page.getByPlaceholder(/ask about events/i)).toBeVisible();

      await page.getByRole('button', { name: /close chat/i }).click();
      await expect(page.getByPlaceholder(/ask about events/i)).not.toBeVisible();
    });
  });

  test.describe('Recommendations', () => {
    test('homepage shows AI recommendations section with event links', async ({ page }) => {
      test.setTimeout(60000);
      await page.goto('/');

      // Wait for recommendations heading to appear
      const heading = page.getByRole('heading', { name: /recommended for you/i });
      await expect(heading).toBeVisible({ timeout: 45000 });

      // The section should contain at least one event link
      // Wait a moment for cards to render after heading appears
      await page.waitForTimeout(1000);
      const allEventLinks = page.locator('a[href^="/events/"]');
      const count = await allEventLinks.count();
      expect(count).toBeGreaterThan(0);
    });
  });

  test.describe('Price Prediction', () => {
    // Price prediction can be slow under concurrent browser load
    test.describe.configure({ retries: 1 });

    test('event detail page shows AI price prediction with trend', async ({ page, request }) => {
      test.setTimeout(60000);

      // Fetch a real event slug from the API
      const eventsResponse = await request.get('http://localhost:8080/api/v1/events');
      const events = await eventsResponse.json();
      const slug = events.content[0].slug;

      // Navigate directly to the event detail page
      await page.goto(`/events/${slug}`);

      // Wait for the price prediction badge
      await expect(page.getByText('AI Price Prediction')).toBeVisible({ timeout: 45000 });

      // Verify a trend label is present (Rising, Falling, or Stable)
      await expect(
        page.getByText('Rising').or(page.getByText('Falling')).or(page.getByText('Stable')),
      ).toBeVisible();
    });
  });
});
