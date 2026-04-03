import { describe, it, expect, vi } from 'vitest';
import { renderWithProviders, screen } from '@/test/test-utils';
import { AdminDashboardPage } from './AdminDashboardPage';

const mockStats = {
  totalUsers: 150,
  totalOrders: 42,
  totalRevenue: 12500.5,
  activeEvents: 8,
  usersTrend: 5.2,
  ordersTrend: -1.3,
  revenueTrend: 12.0,
  eventsTrend: null,
};

const mockOrders = {
  content: [
    {
      id: 1,
      orderNumber: 'ORD-001',
      userName: 'Jane Doe',
      userEmail: 'jane@example.com',
      status: 'COMPLETED',
      total: 199.99,
      itemCount: 2,
      createdAt: '2026-03-15T10:00:00Z',
    },
    {
      id: 2,
      orderNumber: 'ORD-002',
      userName: 'Bob Smith',
      userEmail: 'bob@example.com',
      status: 'PENDING',
      total: 75.0,
      itemCount: 1,
      createdAt: '2026-03-16T14:30:00Z',
    },
  ],
  totalPages: 1,
  number: 0,
  first: true,
  last: true,
};

let statsReturn = { data: mockStats, isLoading: false };
let ordersReturn = { data: mockOrders, isLoading: false };

vi.mock('@/hooks/use-admin', () => ({
  useDashboardStats: () => statsReturn,
  useAdminOrders: () => ordersReturn,
}));

describe('AdminDashboardPage', () => {
  it('renders loading skeletons when data is loading', () => {
    statsReturn = { data: undefined as never, isLoading: true };
    ordersReturn = { data: undefined as never, isLoading: true };

    renderWithProviders(<AdminDashboardPage />);

    expect(screen.getByText('Dashboard')).toBeDefined();
    expect(screen.getByText('Recent Orders')).toBeDefined();
  });

  it('renders stats cards with data', () => {
    statsReturn = { data: mockStats, isLoading: false };
    ordersReturn = { data: mockOrders, isLoading: false };

    renderWithProviders(<AdminDashboardPage />);

    expect(screen.getByText('Total Users')).toBeDefined();
    expect(screen.getByText('150')).toBeDefined();
    expect(screen.getByText('Total Orders')).toBeDefined();
    expect(screen.getByText('42')).toBeDefined();
    expect(screen.getByText('Active Events')).toBeDefined();
    expect(screen.getByText('8')).toBeDefined();
    expect(screen.getByText('Revenue')).toBeDefined();
  });

  it('renders trend indicators on stats cards', () => {
    statsReturn = { data: mockStats, isLoading: false };
    ordersReturn = { data: mockOrders, isLoading: false };

    renderWithProviders(<AdminDashboardPage />);

    expect(screen.getByText('+5.2% from last period')).toBeDefined();
    expect(screen.getByText('-1.3% from last period')).toBeDefined();
  });

  it('renders recent orders table with data', () => {
    statsReturn = { data: mockStats, isLoading: false };
    ordersReturn = { data: mockOrders, isLoading: false };

    renderWithProviders(<AdminDashboardPage />);

    expect(screen.getByText('ORD-001')).toBeDefined();
    expect(screen.getByText('Jane Doe')).toBeDefined();
    expect(screen.getByText('jane@example.com')).toBeDefined();
    expect(screen.getByText('COMPLETED')).toBeDefined();

    expect(screen.getByText('ORD-002')).toBeDefined();
    expect(screen.getByText('Bob Smith')).toBeDefined();
    expect(screen.getByText('PENDING')).toBeDefined();
  });

  it('renders empty state when there are no orders', () => {
    statsReturn = { data: mockStats, isLoading: false };
    ordersReturn = {
      data: { ...mockOrders, content: [] },
      isLoading: false,
    };

    renderWithProviders(<AdminDashboardPage />);

    expect(screen.getByText('No orders yet.')).toBeDefined();
  });
});
