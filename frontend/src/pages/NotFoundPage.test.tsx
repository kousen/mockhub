import { describe, it, expect } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { NotFoundPage } from './NotFoundPage';

describe('NotFoundPage', () => {
  it('renders 404 heading', () => {
    renderWithProviders(<NotFoundPage />);

    expect(screen.getByText('404')).toBeDefined();
  });

  it('renders "Page Not Found" message', () => {
    renderWithProviders(<NotFoundPage />);

    expect(screen.getByText('Page Not Found')).toBeDefined();
  });

  it('renders description text', () => {
    renderWithProviders(<NotFoundPage />);

    expect(
      screen.getByText('The page you are looking for does not exist or has been moved.'),
    ).toBeDefined();
  });

  it('renders "Back to Home" link', () => {
    renderWithProviders(<NotFoundPage />);

    expect(screen.getByText('Back to Home')).toBeDefined();
  });
});
