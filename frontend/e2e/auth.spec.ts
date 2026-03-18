import { test, expect } from '@playwright/test';

test.describe('Authentication flows', () => {
  test('login page renders correctly', async ({ page }) => {
    await page.goto('/login');

    await expect(page.getByText('Welcome back')).toBeVisible();
    await expect(page.getByLabel('Email')).toBeVisible();
    await expect(page.getByLabel('Password')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Log in' })).toBeVisible();
  });

  test('register page renders correctly', async ({ page }) => {
    await page.goto('/register');

    await expect(page.locator('[data-slot="card-title"]', { hasText: 'Create an account' })).toBeVisible();
    await expect(page.getByLabel('Email')).toBeVisible();
    await expect(page.getByLabel('Password', { exact: true })).toBeVisible();
    await expect(page.getByLabel('Confirm password')).toBeVisible();
  });

  test('login page has link to register', async ({ page }) => {
    await page.goto('/login');

    const signUpLink = page.getByRole('main').getByRole('link', { name: 'Sign up' });
    await expect(signUpLink).toBeVisible();
  });

  test('login form requires email', async ({ page }) => {
    await page.goto('/login');

    const emailInput = page.getByLabel('Email');
    await expect(emailInput).toHaveAttribute('required', '');
  });

  test('login form requires password', async ({ page }) => {
    await page.goto('/login');

    const passwordInput = page.getByLabel('Password');
    await expect(passwordInput).toHaveAttribute('required', '');
  });

  test('navigating to protected page without auth redirects', async ({ page }) => {
    await page.goto('/orders');

    // Should be redirected to login or show unauthorized state
    await expect(page).not.toHaveURL('/orders');
  });
});
