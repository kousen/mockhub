export const APP_NAME = 'MockHub';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  EVENTS: '/events',
  EVENT_DETAIL: '/events/:slug',
  CART: '/cart',
  CHECKOUT: '/checkout',
  ORDERS: '/orders',
  FAVORITES: '/favorites',
  PROFILE: '/my/profile',
  SELL: '/sell',
  MY_LISTINGS: '/my/listings',
  EARNINGS: '/my/earnings',
  ADMIN: '/admin',
  ADMIN_DASHBOARD: '/admin',
  ADMIN_EVENTS: '/admin/events',
  ADMIN_EVENTS_NEW: '/admin/events/new',
  ADMIN_USERS: '/admin/users',
} as const;
