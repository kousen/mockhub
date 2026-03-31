import { afterAll, afterEach, beforeAll } from 'vitest';
import { cleanup } from '@testing-library/react';
import { server } from './mocks/server';

// Radix UI components (Tooltip, Popover, etc.) use ResizeObserver which jsdom doesn't provide
global.ResizeObserver = class {
  observe() {
    /* stub for jsdom */
  }
  unobserve() {
    /* stub for jsdom */
  }
  disconnect() {
    /* stub for jsdom */
  }
};

// Start MSW server before all tests
beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }));

// Reset handlers between tests
afterEach(() => {
  cleanup();
  server.resetHandlers();
});

// Close MSW server after all tests
afterAll(() => server.close());
