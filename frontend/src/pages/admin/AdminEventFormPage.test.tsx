import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen, userEvent } from '@/test/test-utils';
import { AdminEventFormPage } from './AdminEventFormPage';

const mockCategories = [
  { id: 1, name: 'Jazz', slug: 'jazz', icon: null, sortOrder: 1 },
  { id: 2, name: 'Rock', slug: 'rock', icon: null, sortOrder: 2 },
];

const mockCreateMutate = vi.fn();
const mockUpdateMutate = vi.fn();

let mockParamsReturn: Record<string, string> = {};
let adminEventReturn: { data: unknown; isLoading: boolean } = {
  data: undefined,
  isLoading: false,
};

vi.mock('react-router', async () => {
  const actual = await vi.importActual('react-router');
  return {
    ...actual,
    useParams: () => mockParamsReturn,
    useNavigate: () => vi.fn(),
  };
});

vi.mock('@/hooks/use-admin', () => ({
  useAdminEvent: () => adminEventReturn,
  useCreateEvent: () => ({
    mutate: mockCreateMutate,
    isPending: false,
  }),
  useUpdateEvent: () => ({
    mutate: mockUpdateMutate,
    isPending: false,
  }),
}));

vi.mock('@/hooks/use-events', () => ({
  useCategories: () => ({
    data: mockCategories,
    isLoading: false,
  }),
}));

describe('AdminEventFormPage - create mode', () => {
  it('renders create mode with empty form', () => {
    mockParamsReturn = {};
    adminEventReturn = { data: undefined, isLoading: false };

    renderWithProviders(<AdminEventFormPage />);

    // "Create Event" appears as both heading and submit button
    const createTexts = screen.getAllByText('Create Event');
    expect(createTexts.length).toBe(2);
    expect(screen.getByText('Event Details')).toBeDefined();
    expect(screen.getByLabelText('Event Name *')).toBeDefined();
    expect(screen.getByLabelText('Artist Name')).toBeDefined();
    expect(screen.getByLabelText('Venue *')).toBeDefined();
    expect(screen.getByLabelText('Event Date & Time *')).toBeDefined();
    expect(screen.getByLabelText('Base Price (USD) *')).toBeDefined();
    expect(screen.getByLabelText('Description')).toBeDefined();

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDefined();
  });

  it('accepts user input in form fields', async () => {
    mockParamsReturn = {};
    adminEventReturn = { data: undefined, isLoading: false };
    const user = userEvent.setup();

    renderWithProviders(<AdminEventFormPage />);

    const nameInput = screen.getByLabelText('Event Name *');
    await user.type(nameInput, 'New Concert');
    expect(nameInput).toHaveProperty('value', 'New Concert');

    const artistInput = screen.getByLabelText('Artist Name');
    await user.type(artistInput, 'Great Band');
    expect(artistInput).toHaveProperty('value', 'Great Band');
  });

  it('renders category select placeholder', () => {
    mockParamsReturn = {};
    adminEventReturn = { data: undefined, isLoading: false };

    renderWithProviders(<AdminEventFormPage />);

    expect(screen.getByText('Select a category')).toBeDefined();
  });
});

describe('AdminEventFormPage - edit mode', () => {
  const mockExistingEvent = {
    id: 5,
    name: 'Existing Concert',
    slug: 'existing-concert',
    artistName: 'Test Artist',
    status: 'ACTIVE',
    eventDate: '2026-08-01T19:00:00Z',
    doorsOpenAt: '2026-08-01T18:00:00Z',
    basePrice: 99.99,
    minPrice: 50.0,
    maxPrice: 200.0,
    totalTickets: 300,
    availableTickets: 250,
    isFeatured: false,
    description: 'A great concert',
    venue: {
      id: 1,
      name: 'Central Park',
      slug: 'central-park',
      city: 'New York',
      state: 'NY',
      venueType: 'OUTDOOR',
      capacity: 5000,
      imageUrl: null,
    },
    category: { id: 1, name: 'Jazz', slug: 'jazz', icon: null, sortOrder: 1 },
    tags: [],
    primaryImageUrl: null,
    spotifyArtistId: null,
  };

  it('renders edit mode heading and Update button', () => {
    mockParamsReturn = { id: '5' };
    adminEventReturn = { data: mockExistingEvent, isLoading: false };

    renderWithProviders(<AdminEventFormPage />);

    expect(screen.getByText('Edit Event')).toBeDefined();
    expect(screen.getByRole('button', { name: 'Update Event' })).toBeDefined();
  });

  it('shows loading skeleton in edit mode when event is loading', () => {
    mockParamsReturn = { id: '5' };
    adminEventReturn = { data: undefined, isLoading: true };

    const { container } = renderWithProviders(<AdminEventFormPage />);

    // When loading in edit mode, a full-page skeleton is rendered
    // and the form is not shown
    expect(screen.queryByText('Event Details')).toBeNull();
    expect(container.querySelector('.h-96')).toBeDefined();
  });
});
