import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { LoadingSpinner } from './LoadingSpinner';

describe('LoadingSpinner', () => {
  it('renders an output element with aria-label', () => {
    render(<LoadingSpinner />);
    const output = screen.getByRole('status');
    expect(output).toBeDefined();
    expect(output.getAttribute('aria-label')).toBe('Loading');
  });

  it('renders screen-reader text', () => {
    render(<LoadingSpinner />);
    expect(screen.getByText('Loading...')).toBeDefined();
  });

  it('applies default medium size classes', () => {
    render(<LoadingSpinner />);
    const output = screen.getByRole('status');
    expect(output.className).toContain('h-8');
    expect(output.className).toContain('w-8');
  });

  it('applies small size classes when size="sm"', () => {
    render(<LoadingSpinner size="sm" />);
    const output = screen.getByRole('status');
    expect(output.className).toContain('h-4');
    expect(output.className).toContain('w-4');
  });

  it('applies large size classes when size="lg"', () => {
    render(<LoadingSpinner size="lg" />);
    const output = screen.getByRole('status');
    expect(output.className).toContain('h-12');
    expect(output.className).toContain('w-12');
  });

  it('applies custom className to the wrapper', () => {
    const { container } = render(<LoadingSpinner className="mt-8" />);
    const wrapper = container.firstElementChild as HTMLElement;
    expect(wrapper.className).toContain('mt-8');
  });
});
