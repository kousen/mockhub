import { http, HttpResponse } from 'msw';

const API_BASE = 'http://localhost:8080/api/v1';

const mockEvents = [
  {
    id: 1,
    name: 'Rock Festival 2026',
    slug: 'rock-festival-2026',
    artistName: 'The Rockers',
    venueName: 'Madison Square Garden',
    city: 'New York',
    eventDate: '2026-06-15T20:00:00Z',
    minPrice: 75.0,
    availableTickets: 500,
    primaryImageUrl: null,
    categoryName: 'Concert',
    isFeatured: true,
  },
  {
    id: 2,
    name: 'Jazz Night',
    slug: 'jazz-night',
    artistName: 'Jazz Ensemble',
    venueName: 'Blue Note',
    city: 'New York',
    eventDate: '2026-07-20T19:00:00Z',
    minPrice: 45.0,
    availableTickets: 100,
    primaryImageUrl: null,
    categoryName: 'Jazz',
    isFeatured: false,
  },
];

const mockUser = {
  id: 1,
  email: 'test@example.com',
  firstName: 'John',
  lastName: 'Doe',
  phone: null,
  avatarUrl: null,
  emailVerified: false,
  roles: ['ROLE_BUYER'],
  createdAt: '2026-01-01T00:00:00Z',
};

const mockCart = {
  id: 1,
  userId: 1,
  items: [],
  subtotal: 0,
  itemCount: 0,
  expiresAt: null,
};

const mockNotifications = {
  content: [
    {
      id: 1,
      type: 'ORDER_CONFIRMED',
      title: 'Order Confirmed',
      message: 'Your order MH-20260315-0001 has been confirmed.',
      link: '/orders/MH-20260315-0001',
      isRead: false,
      createdAt: '2026-03-15T10:00:00Z',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
};

export const handlers = [
  // Auth
  http.post(`${API_BASE}/auth/login`, () => {
    return HttpResponse.json({
      accessToken: 'mock-jwt-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: mockUser,
    });
  }),

  http.post(`${API_BASE}/auth/register`, () => {
    return HttpResponse.json(
      {
        accessToken: 'mock-jwt-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
        user: mockUser,
      },
      { status: 201 },
    );
  }),

  http.get(`${API_BASE}/auth/me`, () => {
    return HttpResponse.json(mockUser);
  }),

  // Events
  http.get(`${API_BASE}/events`, () => {
    return HttpResponse.json({
      content: mockEvents,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    });
  }),

  http.get(`${API_BASE}/events/featured`, () => {
    return HttpResponse.json(mockEvents.filter((e) => e.isFeatured));
  }),

  http.get(`${API_BASE}/events/:slug`, ({ params }) => {
    const event = mockEvents.find((e) => e.slug === params.slug);
    if (!event) {
      return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    }
    return HttpResponse.json({
      ...event,
      description: 'A great event',
      doorsOpenAt: null,
      status: 'ACTIVE',
      basePrice: event.minPrice,
      maxPrice: event.minPrice ? event.minPrice * 2 : null,
      totalTickets: 1000,
      isFeatured: event.isFeatured,
      venue: {
        id: 1,
        name: event.venueName,
        slug: 'madison-square-garden',
        city: event.city,
        state: 'NY',
        venueType: 'ARENA',
        capacity: 20000,
        imageUrl: null,
      },
      category: { id: 1, name: event.categoryName, slug: 'concert', icon: 'music', sortOrder: 1 },
      tags: [],
    });
  }),

  // Categories
  http.get(`${API_BASE}/categories`, () => {
    return HttpResponse.json([
      { id: 1, name: 'Concert', slug: 'concert', icon: 'music', sortOrder: 1 },
      { id: 2, name: 'Sports', slug: 'sports', icon: 'trophy', sortOrder: 2 },
    ]);
  }),

  // Cart
  http.get(`${API_BASE}/cart`, () => {
    return HttpResponse.json(mockCart);
  }),

  http.post(`${API_BASE}/cart/items`, () => {
    return HttpResponse.json({ ...mockCart, itemCount: 1, subtotal: 75.0 }, { status: 201 });
  }),

  http.delete(`${API_BASE}/cart/items/:id`, () => {
    return HttpResponse.json(mockCart);
  }),

  http.delete(`${API_BASE}/cart`, () => {
    return new HttpResponse(null, { status: 204 });
  }),

  // Favorites
  http.get(`${API_BASE}/favorites`, () => {
    return HttpResponse.json([]);
  }),

  http.get(`${API_BASE}/favorites/check/:eventId`, () => {
    return HttpResponse.json({ favorited: false });
  }),

  http.post(`${API_BASE}/favorites/:eventId`, () => {
    return new HttpResponse(null, { status: 201 });
  }),

  http.delete(`${API_BASE}/favorites/:eventId`, () => {
    return new HttpResponse(null, { status: 204 });
  }),

  // Orders
  http.get(`${API_BASE}/orders`, () => {
    return HttpResponse.json({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
  }),

  // Notifications
  http.get(`${API_BASE}/notifications`, () => {
    return HttpResponse.json(mockNotifications);
  }),

  http.get(`${API_BASE}/notifications/unread-count`, () => {
    return HttpResponse.json({ count: 1 });
  }),

  // Search
  http.get(`${API_BASE}/search`, () => {
    return HttpResponse.json({
      content: mockEvents,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    });
  }),
];
