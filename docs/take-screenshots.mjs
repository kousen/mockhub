/**
 * Captures screenshots of MockHub for the README.
 *
 * Prerequisites:
 *   - Database running: docker compose -f docker-compose.dev.yml up -d
 *   - Backend running with AI: SPRING_PROFILES_ACTIVE=dev,ai-anthropic ./gradlew bootRun
 *   - Frontend running: cd frontend && npm run dev
 *
 * Usage:
 *   node docs/take-screenshots.mjs
 *
 * Screenshots are saved to docs/screenshots/ and referenced in README.md.
 */
import { chromium } from './screenshot-deps.mjs';

const DIR = './docs/screenshots';

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });

// 1. Homepage
console.log('1/5 Homepage...');
await page.goto('http://localhost:5173/');
await page.waitForTimeout(2000);
await page.screenshot({ path: `${DIR}/homepage.png` });

// 2. Recommendations (wait for AI to load cards)
console.log('2/5 Recommendations...');
try {
  await page.getByRole('heading', { name: /recommended for you/i }).waitFor({ timeout: 45000 });
  const section = page.locator('section', { hasText: 'Recommended for You' });
  await section.locator('a[href^="/events/"]').first().waitFor({ timeout: 15000 });
  await page.getByRole('heading', { name: /recommended for you/i }).scrollIntoViewIfNeeded();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: `${DIR}/recommendations.png` });
} catch {
  console.log('  skipped (AI not loaded — is the backend running with an AI profile?)');
}

// 3. Event detail with price prediction
console.log('3/5 Event detail...');
await page.goto('http://localhost:5173/events');
await page.waitForTimeout(2000);
const href = await page.locator('a[href^="/events/"]').first().getAttribute('href');
await page.goto(`http://localhost:5173${href}`);
try {
  await page.getByText('AI Price Prediction').waitFor({ timeout: 30000 });
} catch {
  console.log('  price prediction not loaded');
}
await page.waitForTimeout(1000);
await page.screenshot({ path: `${DIR}/event-detail.png` });

// 4. Chat widget with conversation
console.log('4/5 Chat widget...');
await page.goto('http://localhost:5173/');
await page.waitForTimeout(1000);
await page.getByRole('button', { name: /open ai chat assistant/i }).click();
const input = page.getByPlaceholder(/ask about events/i);
await input.fill('What events are near Connecticut?');
await input.press('Enter');
try {
  await page.locator('[data-testid="ai-message"]').first().waitFor({ timeout: 30000 });
  await page.waitForTimeout(500);
} catch {
  console.log('  chat response not received');
}
await page.screenshot({ path: `${DIR}/chat-widget.png` });

// 5. Events browse page
console.log('5/5 Events browse...');
await page.goto('http://localhost:5173/events');
await page.waitForTimeout(2000);
await page.screenshot({ path: `${DIR}/events-browse.png` });

await browser.close();
console.log(`Done! Screenshots saved to ${DIR}/`);
