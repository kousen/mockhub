import { test, expect, type Page } from '@playwright/test';

// Mock AI responses for deterministic, fast E2E tests.
// In CI, the backend runs without an AI provider — these mocks
// ensure the frontend components render correctly regardless.
// For local testing with real AI, run without CI=true.

const MOCK_CHAT_RESPONSE = {
  conversationId: null,
  message:
    'Here are some great events coming up! Check out [Rock Festival](/events/rock-festival) at Madison Square Garden.',
  timestamp: new Date().toISOString(),
};

const MOCK_RECOMMENDATIONS = [
  {
    eventId: 1,
    eventName: 'Rock Festival 2026',
    eventSlug: 'rock-festival-2026',
    venueName: 'Madison Square Garden',
    city: 'New York',
    eventDate: '2026-06-15T19:00:00Z',
    minPrice: 75.0,
    relevanceScore: 0.95,
    reason: 'Popular rock event with high demand',
  },
  {
    eventId: 2,
    eventName: 'Jazz Night',
    eventSlug: 'jazz-night',
    venueName: 'Blue Note',
    city: 'New York',
    eventDate: '2026-07-01T20:00:00Z',
    minPrice: 45.0,
    relevanceScore: 0.82,
    reason: 'Intimate jazz venue experience',
  },
];

const MOCK_PRICE_PREDICTION = {
  eventSlug: 'test-event',
  currentPrice: 75.0,
  predictedPrice: 82.5,
  trend: 'RISING',
  confidence: 0.72,
  predictedAt: new Date().toISOString(),
};

async function mockAiEndpoints(page: Page) {
  await page.route('**/api/v1/chat', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_CHAT_RESPONSE),
    });
  });

  await page.route('**/api/v1/recommendations', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_RECOMMENDATIONS),
    });
  });

  await page.route('**/api/v1/events/*/predicted-price', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_PRICE_PREDICTION),
    });
  });
}

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

    test('sending a message returns a response', async ({ page }) => {
      await mockAiEndpoints(page);
      await page.goto('/');

      await page.getByRole('button', { name: /open ai chat assistant/i }).click();

      const input = page.getByPlaceholder(/ask about events/i);
      await input.fill('What events are available?');
      await input.press('Enter');

      const aiMessage = page.locator('[data-testid="ai-message"]').first();
      await expect(aiMessage).toBeVisible({ timeout: 10000 });
      await expect(aiMessage).not.toBeEmpty();
    });

    test('chat response contains clickable event links', async ({ page }) => {
      await mockAiEndpoints(page);
      await page.goto('/');

      await page.getByRole('button', { name: /open ai chat assistant/i }).click();

      const input = page.getByPlaceholder(/ask about events/i);
      await input.fill('What events are near me?');
      await input.press('Enter');

      const aiMessage = page.locator('[data-testid="ai-message"]').first();
      await expect(aiMessage).toBeVisible({ timeout: 10000 });

      // The mock response contains a markdown link that should be rendered
      const link = aiMessage.locator('a[href^="/events/"]');
      await expect(link).toBeVisible();
    });

    test('chat panel can be closed', async ({ page }) => {
      await page.goto('/');

      await page.getByRole('button', { name: /open ai chat assistant/i }).click();
      await expect(page.getByPlaceholder(/ask about events/i)).toBeVisible();

      await page.getByRole('button', { name: /close chat/i }).click();
      await expect(page.getByPlaceholder(/ask about events/i)).not.toBeVisible();
    });
  });

  test.describe('Price Prediction', () => {
    test('event detail page shows AI price prediction with trend', async ({ page }) => {
      await mockAiEndpoints(page);

      // Navigate to events page and click the first event
      await page.goto('/events');
      const eventLink = page.locator('a[href^="/events/"]').first();
      await expect(eventLink).toBeVisible({ timeout: 10000 });
      const href = await eventLink.getAttribute('href');
      await page.goto(href!);

      await expect(page.getByText('AI Price Prediction')).toBeVisible({ timeout: 10000 });

      await expect(
        page.getByText('Rising').or(page.getByText('Falling')).or(page.getByText('Stable')),
      ).toBeVisible();
    });
  });
});
