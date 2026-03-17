import { setupServer } from 'msw/node';
import { handlers } from './handlers';

/**
 * MSW server instance for Vitest.
 * Started/stopped in the test setup file.
 */
export const server = setupServer(...handlers);
